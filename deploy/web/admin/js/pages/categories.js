/* 分类管理 */
import { api } from '../api.js';
import { $, escapeHtml, fmtDate } from '../util.js';
import { bindPager, confirmDialog, emptyTable, openModal, pagerHtml, setBtnLoading, statusTag, toast, toastError } from '../ui.js';

const state = { page: 1, pageSize: 10, name: '', type: '', total: 0 };
const TYPE_TEXT = { 1: '菜品分类', 2: '套餐分类' };

function renderTable(data) {
  const holder = $('#category-table');
  const list = (data && data.list) || [];
  const rows = list.length ? list.map((c) => `
    <tr>
      <td class="cell-strong">${escapeHtml(c.name || '')}</td>
      <td>${statusTag(TYPE_TEXT[c.type] || `类型 ${c.type}`, c.type === 2 ? 'info' : 'brand')}</td>
      <td class="num">${c.sort == null ? '—' : c.sort}</td>
      <td><button class="switch ${c.status === 1 ? 'on' : ''}" data-status="${c.id}" data-current="${c.status}" type="button" aria-label="启用禁用"></button></td>
      <td class="text-sm muted">${escapeHtml(fmtDate(c.createTime))}</td>
      <td><div class="row-actions">
        <button class="btn btn-ghost btn-xs" data-edit="${c.id}">编辑</button>
        <button class="btn btn-danger-ghost btn-xs" data-delete="${c.id}">删除</button>
      </div></td>
    </tr>`).join('') : emptyTable(6, '还没有分类', '点击右上角「新增分类」开始配置');
  const pager = pagerHtml(data ? data.total : 0, state.page, state.pageSize);
  holder.innerHTML = `<div class="table-wrap"><table class="data-table">
    <thead><tr><th style="min-width:180px">分类名称</th><th style="width:120px">类型</th><th style="width:80px">排序</th><th style="width:80px">状态</th><th style="width:150px">创建时间</th><th style="width:130px">操作</th></tr></thead>
    <tbody>${rows}</tbody></table></div>${pager.html}`;
  bindPager(holder, (page) => { state.page = page; loadList(); });
}

async function loadList() {
  const holder = $('#category-table');
  holder.innerHTML = '<div class="table-empty">加载中…</div>';
  try {
    const data = await api.categoryPage({
      page: state.page, pageSize: state.pageSize,
      name: state.name, type: state.type === '' ? undefined : Number(state.type)
    });
    state.total = data ? data.total : 0;
    renderTable(data);
  } catch (error) {
    toastError(error);
    holder.innerHTML = '<div class="table-empty">分类加载失败</div>';
  }
}

function openForm(category) {
  const isEdit = !!category;
  openModal({
    title: isEdit ? '编辑分类' : '新增分类',
    subtitle: '菜品与套餐分别维护各自的分类',
    size: 'modal-sm',
    body: `<form id="cat-form" class="form-grid" style="grid-template-columns:1fr" autocomplete="off">
      <div class="form-item"><label>分类类型<em>*</em></label>
        <select class="select" name="type" ${isEdit ? 'disabled' : ''}>
          <option value="1" ${category?.type === 1 || !category ? 'selected' : ''}>菜品分类（type=1）</option>
          <option value="2" ${category?.type === 2 ? 'selected' : ''}>套餐分类（type=2）</option>
        </select>
        ${isEdit ? '<span class="form-hint">编辑接口不修改类型，仅新增时可选</span>' : ''}
      </div>
      <div class="form-item"><label>分类名称<em>*</em></label><input class="input" name="name" maxlength="32" value="${escapeHtml(category?.name || '')}" placeholder="例如：热销推荐"></div>
      <div class="form-item"><label>排序</label><input class="input" name="sort" type="number" step="1" value="${category?.sort ?? 0}" placeholder="数字越小越靠前"><span class="form-hint">用于列表展示顺序</span></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button>
             <button class="btn btn-primary" type="button" data-save>${isEdit ? '保存修改' : '创建分类'}</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#cat-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const name = form.elements.namedItem('name').value.trim();
        const sort = Number(form.sort.value);
        const type = isEdit ? Number(category.type) : Number(form.type.value);
        if (!name) { errorEl.hidden = false; errorEl.textContent = '请填写分类名称'; return; }
        if (![1, 2].includes(type)) { errorEl.hidden = false; errorEl.textContent = '请选择分类类型'; return; }
        const payload = { name, sort: Number.isFinite(sort) ? sort : 0 };
        if (isEdit) payload.id = category.id; else payload.type = type;
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          if (isEdit) await api.categoryUpdate(payload); else await api.categorySave(payload);
          toast(isEdit ? '分类已更新' : '分类已创建', 'success');
          close();
          await loadList();
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      });
    }
  });
}

async function editCategory(id) {
  try {
    const list = await api.categoryList();
    const category = (list || []).find((c) => Number(c.id) === Number(id));
    if (!category) throw new Error('未找到该分类，可能已被删除');
    openForm(category);
  } catch (error) { toastError(error); }
}

async function toggleStatus(id, current) {
  const next = Number(current) === 1 ? 0 : 1;
  try {
    await api.categoryStatus(next, id);
    toast(next === 1 ? '分类已启用' : '分类已禁用', 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

async function removeCategory(id) {
  const ok = await confirmDialog({ title: '删除分类', message: `确定删除分类 <strong>#${id}</strong> 吗？分类下存在菜品时后端会拒绝删除。`, danger: true, confirmText: '删除' });
  if (!ok) return;
  try {
    await api.categoryDelete(id);
    toast('分类已删除', 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

export async function render(root) {
  Object.assign(state, { page: 1, pageSize: 10, name: '', type: '', total: 0 });
  root.innerHTML = `
    <div class="section-stack">
      <div class="card">
        <div class="toolbar">
          <div class="toolbar-left">
            <input id="f-name" class="input filter-input" placeholder="分类名称" autocomplete="off">
            <select id="f-type" class="select filter-select">
              <option value="">全部类型</option><option value="1">菜品分类</option><option value="2">套餐分类</option>
            </select>
            <button id="btn-search" class="btn btn-primary" type="button">查询</button>
            <button id="btn-reset" class="btn btn-ghost" type="button">重置</button>
          </div>
          <div class="toolbar-right">
            <button id="btn-add" class="btn btn-primary" type="button">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>新增分类
            </button>
          </div>
        </div>
        <div id="category-table"><div class="table-empty">加载中…</div></div>
      </div>
    </div>`;

  $('#btn-search').addEventListener('click', () => {
    state.page = 1;
    state.name = $('#f-name').value.trim();
    state.type = $('#f-type').value;
    loadList();
  });
  $('#btn-reset').addEventListener('click', () => {
    $('#f-name').value = ''; $('#f-type').value = '';
    state.page = 1; state.name = ''; state.type = '';
    loadList();
  });
  $('#f-name').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('#btn-search').click(); });
  $('#btn-add').addEventListener('click', () => openForm(null));
  $('#category-table').addEventListener('click', (event) => {
    const edit = event.target.closest('[data-edit]');
    const del = event.target.closest('[data-delete]');
    const status = event.target.closest('[data-status]');
    if (edit) { editCategory(Number(edit.dataset.edit)); return; }
    if (del) { removeCategory(Number(del.dataset.delete)); return; }
    if (status) { toggleStatus(Number(status.dataset.status), status.dataset.current); }
  });

  await loadList();
}

export function destroy() {}


