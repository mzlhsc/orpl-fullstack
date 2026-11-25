/* 轻量 UI 组件：Toast / Modal / Drawer / 分页 / 空态 */
import { escapeHtml } from './util.js';

const modalRoot = () => document.getElementById('modal-root');
const toastRoot = () => document.getElementById('toast-root');
let openLayers = 0;

const ICONS = {
  success: '<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M5 13l4 4L19 7"/></svg>',
  error: '<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.4"><path d="M6 6l12 12M18 6L6 18"/></svg>',
  info: '<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 8v.01M12 11v6"/><circle cx="12" cy="12" r="9"/></svg>',
  warn: '<svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 8v5M12 16v.01"/><path d="M10.3 3.7 2.7 17a2 2 0 0 0 1.7 3h15.2a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0Z"/></svg>'
};

export function toast(message, type = 'info', duration = 3000) {
  const root = toastRoot();
  if (!root) return;
  const item = document.createElement('div');
  item.className = `toast toast-${type}`;
  item.innerHTML = `<span class="toast-ic">${ICONS[type] || ICONS.info}</span><div>${escapeHtml(message)}</div>`;
  root.appendChild(item);
  setTimeout(() => {
    item.style.opacity = '0';
    item.style.transform = 'translateY(-6px)';
    item.style.transition = 'all .18s ease';
    setTimeout(() => item.remove(), 200);
  }, duration);
}

export function toastError(error) {
  const message = error && error.message ? error.message : String(error || '操作失败');
  toast(message, 'error', 4000);
}

function lockScroll() {
  openLayers += 1;
  document.body.style.overflow = 'hidden';
}
function unlockScroll() {
  openLayers = Math.max(0, openLayers - 1);
  if (openLayers === 0) document.body.style.overflow = '';
}

function layerClose(backdrop, onClose) {
  if (!backdrop || backdrop.dataset.closed === '1') return;
  backdrop.dataset.closed = '1';
  backdrop.style.opacity = '0';
  backdrop.style.transition = 'opacity .16s ease';
  setTimeout(() => {
    backdrop.remove();
    unlockScroll();
    if (typeof onClose === 'function') onClose();
  }, 160);
}

export function openModal(options = {}) {
  const {
    title = '', subtitle = '', body = '', footer = '', size = '',
    closeOnBackdrop = true, onMount, onClose, showClose = true
  } = options;
  const root = modalRoot();
  const backdrop = document.createElement('div');
  backdrop.className = 'modal-backdrop';
  backdrop.innerHTML = `
    <div class="modal ${size}" role="dialog" aria-modal="true">
      <div class="modal-header">
        <div><h2>${escapeHtml(title)}</h2>${subtitle ? `<p>${escapeHtml(subtitle)}</p>` : ''}</div>
        ${showClose ? '<button class="modal-close" type="button" data-close aria-label="关闭">×</button>' : ''}
      </div>
      <div class="modal-body"></div>
      ${footer ? '<div class="modal-footer"></div>' : ''}
    </div>`;
  const modal = backdrop.querySelector('.modal');
  const bodyEl = backdrop.querySelector('.modal-body');
  if (body instanceof Node) bodyEl.appendChild(body); else bodyEl.innerHTML = body;
  const footerEl = backdrop.querySelector('.modal-footer');
  if (footerEl) {
    if (footer instanceof Node) footerEl.appendChild(footer); else footerEl.innerHTML = footer;
  }

  const close = () => layerClose(backdrop, onClose);
  backdrop.addEventListener('mousedown', (event) => {
    if (closeOnBackdrop && event.target === backdrop) close();
  });
  backdrop.addEventListener('click', (event) => {
    if (event.target.closest('[data-close]')) close();
  });
  root.appendChild(backdrop);
  lockScroll();
  if (typeof onMount === 'function') onMount(modal, close);
  return { el: modal, body: bodyEl, footer: footerEl, close, backdrop };
}

export function confirmDialog(options = {}) {
  const {
    title = '确认操作', message = '', confirmText = '确定', cancelText = '取消',
    danger = false, subtitle = ''
  } = options;
  return new Promise((resolve) => {
    let resolved = false;
    const done = (value) => { if (!resolved) { resolved = true; resolve(value); } };
    const layer = openModal({
      title, subtitle,
      size: 'modal-sm',
      body: `<div class="confirm-body">${message}</div>`,
      footer: `<button class="btn btn-ghost" type="button" data-cancel>${escapeHtml(cancelText)}</button>
               <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" type="button" data-confirm>${escapeHtml(confirmText)}</button>`,
      onClose: () => done(false),
      onMount: (modal, close) => {
        modal.querySelector('[data-cancel]').addEventListener('click', () => { done(false); close(); });
        modal.querySelector('[data-confirm]').addEventListener('click', () => { done(true); close(); });
      }
    });
    void layer;
  });
}

export function promptDialog(options = {}) {
  const {
    title = '请输入', message = '', label = '', placeholder = '', value = '',
    required = true, multiline = false, confirmText = '确定', danger = false, maxLength = 200
  } = options;
  return new Promise((resolve) => {
    let resolved = false;
    const done = (val) => { if (!resolved) { resolved = true; resolve(val); } };
    const field = multiline
      ? `<textarea class="textarea" data-input maxlength="${maxLength}" placeholder="${escapeHtml(placeholder)}">${escapeHtml(value)}</textarea>`
      : `<input class="input" data-input maxlength="${maxLength}" placeholder="${escapeHtml(placeholder)}" value="${escapeHtml(value)}">`;
    const layer = openModal({
      title,
      size: 'modal-sm',
      body: `<div class="form-item">${label ? `<label>${escapeHtml(label)}${required ? '<em>*</em>' : ''}</label>` : ''}${message ? `<p class="form-hint" style="margin:0 0 8px">${escapeHtml(message)}</p>` : ''}${field}<p class="form-error" data-error hidden></p></div>`,
      footer: `<button class="btn btn-ghost" type="button" data-cancel>取消</button>
               <button class="btn ${danger ? 'btn-danger' : 'btn-primary'}" type="button" data-confirm>${escapeHtml(confirmText)}</button>`,
      onClose: () => done(null),
      onMount: (modal, close) => {
        const input = modal.querySelector('[data-input]');
        const error = modal.querySelector('[data-error]');
        input.focus();
        if (input.setSelectionRange) input.setSelectionRange(input.value.length, input.value.length);
        const submit = () => {
          const val = input.value.trim();
          if (required && !val) {
            error.hidden = false;
            error.textContent = '该项不能为空';
            input.focus();
            return;
          }
          done(val);
          close();
        };
        modal.querySelector('[data-confirm]').addEventListener('click', submit);
        modal.querySelector('[data-cancel]').addEventListener('click', () => { done(null); close(); });
        input.addEventListener('keydown', (e) => {
          if (e.key === 'Enter' && (!multiline || e.ctrlKey)) { e.preventDefault(); submit(); }
        });
      }
    });
    void layer;
  });
}

export function openDrawer(options = {}) {
  const { title = '', subtitle = '', body = '', footer = '', onMount, onClose } = options;
  const root = modalRoot();
  const backdrop = document.createElement('div');
  backdrop.className = 'drawer-backdrop';
  backdrop.innerHTML = `
    <aside class="drawer" role="dialog" aria-modal="true">
      <div class="drawer-header">
        <div><h2>${escapeHtml(title)}</h2>${subtitle ? `<p>${escapeHtml(subtitle)}</p>` : ''}</div>
        <button class="modal-close" type="button" data-close aria-label="关闭">×</button>
      </div>
      <div class="drawer-body"></div>
      ${footer ? '<div class="drawer-footer"></div>' : ''}
    </aside>`;
  const drawer = backdrop.querySelector('.drawer');
  const bodyEl = backdrop.querySelector('.drawer-body');
  if (body instanceof Node) bodyEl.appendChild(body); else bodyEl.innerHTML = body;
  if (footer) {
    const footerEl = backdrop.querySelector('.drawer-footer');
    if (footer instanceof Node) footerEl.appendChild(footer); else footerEl.innerHTML = footer;
  }
  const close = () => layerClose(backdrop, onClose);
  backdrop.addEventListener('mousedown', (e) => { if (e.target === backdrop) close(); });
  backdrop.addEventListener('click', (e) => { if (e.target.closest('[data-close]')) close(); });
  root.appendChild(backdrop);
  lockScroll();
  if (typeof onMount === 'function') onMount(drawer, close);
  return { el: drawer, body: bodyEl, close };
}

export function statusTag(text, tone = 'default') {
  const cls = tone && tone !== 'default' ? `tag-${tone}` : '';
  return `<span class="tag ${cls}"><span class="tag-dot"></span>${escapeHtml(text)}</span>`;
}

export function thumb(url, alt = '') {
  if (url) {
    return `<span style="display:inline-flex"><img class="thumb" src="${escapeHtml(url)}" alt="${escapeHtml(alt)}" loading="lazy" onerror="this.style.display='none';this.nextElementSibling.style.display='grid'"><span class="thumb-placeholder" style="display:none"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M4 16l4.5-4.5 3 3 3-3L20 16"/></svg></span></span>`;
  }
  return `<span class="thumb-placeholder"><svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M4 16l4.5-4.5 3 3 3-3L20 16"/></svg></span>`;
}

export function emptyTable(colspan, text = '暂无数据', sub = '') {
  return `<tr><td colspan="${colspan}"><div class="table-empty">
    <svg viewBox="0 0 64 64" width="54" height="54" fill="none" stroke="currentColor" stroke-width="2">
      <rect x="10" y="16" width="44" height="34" rx="6"/><path d="M10 27h44M22 16v11M42 16v11"/>
      <path d="M24 38h16" stroke-dasharray="3 4"/>
    </svg>
    <div>${escapeHtml(text)}</div>${sub ? `<div class="text-sm" style="margin-top:6px">${escapeHtml(sub)}</div>` : ''}
  </div></td></tr>`;
}

export function pagerHtml(total, page, pageSize) {
  const totalPages = Math.max(1, Math.ceil((Number(total) || 0) / (Number(pageSize) || 10)));
  const current = Math.min(Math.max(1, Number(page) || 1), totalPages);
  const pages = [];
  const push = (p) => { if (!pages.includes(p)) pages.push(p); };
  push(1);
  for (let p = current - 2; p <= current + 2; p += 1) if (p > 1 && p < totalPages) push(p);
  if (totalPages > 1) push(totalPages);
  pages.sort((a, b) => a - b);

  let buttons = `<button type="button" data-page="${current - 1}" ${current <= 1 ? 'disabled' : ''} aria-label="上一页">‹</button>`;
  let last = 0;
  pages.forEach((p) => {
    if (p - last > 1) buttons += '<span class="pager-ellipsis">…</span>';
    buttons += `<button type="button" data-page="${p}" class="${p === current ? 'active' : ''}">${p}</button>`;
    last = p;
  });
  buttons += `<button type="button" data-page="${current + 1}" ${current >= totalPages ? 'disabled' : ''} aria-label="下一页">›</button>`;

  return {
    totalPages,
    html: `<div class="table-foot">
      <span class="foot-info">共 <strong class="num">${Number(total) || 0}</strong> 条 · 第 ${current}/${totalPages} 页</span>
      <div class="pager">${buttons}</div>
    </div>`
  };
}

export function bindPager(container, onGo) {
  const pager = container.querySelector('.pager');
  if (!pager) return;
  pager.addEventListener('click', (event) => {
    const btn = event.target.closest('button[data-page]');
    if (!btn || btn.disabled) return;
    const page = Number(btn.dataset.page);
    if (Number.isFinite(page) && page > 0) onGo(page);
  });
}

export function setBtnLoading(button, loading, loadingText = '处理中…') {
  if (!button) return;
  if (loading) {
    if (!button.dataset.originalHtml) button.dataset.originalHtml = button.innerHTML;
    button.disabled = true;
    button.innerHTML = `<span class="btn-spinner" style="border-color:rgba(0,0,0,.2);border-top-color:currentColor"></span>${escapeHtml(loadingText)}`;
  } else {
    button.disabled = false;
    if (button.dataset.originalHtml) {
      button.innerHTML = button.dataset.originalHtml;
      delete button.dataset.originalHtml;
    }
  }
}

export function bindRowActions(container, selector, handler) {
  container.addEventListener('click', (event) => {
    const el = event.target.closest(selector);
    if (!el || !container.contains(el)) return;
    handler(el, event);
  });
}
