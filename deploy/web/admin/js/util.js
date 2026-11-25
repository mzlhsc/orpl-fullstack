/* 通用工具函数 */
export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

export function escapeHtml(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function money(value, symbol = '¥') {
  const n = Number(value);
  if (!Number.isFinite(n)) return `${symbol}0.00`;
  return symbol + n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

export function number(value, digits = 0) {
  const n = Number(value);
  if (!Number.isFinite(n)) return '0';
  return n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits });
}

export function percent(value, digits = 1) {
  const n = Number(value);
  if (!Number.isFinite(n)) return '0%';
  return (n * 100).toFixed(digits) + '%';
}

export function pad2(n) { return String(n).padStart(2, '0'); }

export function fmtDate(value, withTime = true) {
  if (!value) return '—';
  const raw = String(value);
  // 后端 LocalDateTime 序列化为 2026-11-24T10:30:00
  const m = raw.match(/^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/);
  if (m) {
    const date = `${m[1]}-${m[2]}-${m[3]}`;
    if (!withTime) return date;
    if (m[4]) return `${date} ${m[4]}:${m[5]}`;
    return date;
  }
  const d = new Date(raw);
  if (Number.isNaN(d.getTime())) return raw;
  const date = `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
  return withTime ? `${date} ${pad2(d.getHours())}:${pad2(d.getMinutes())}` : date;
}

export function dateOnly(value) { return fmtDate(value, false); }

export function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

export function shiftDate(days, from = new Date()) {
  const d = new Date(from);
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

export function toDateTimeInput(value = new Date()) {
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return '';
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
}

export function fromDateTimeInput(value) {
  if (!value) return '';
  return value.replace('T', ' ') + (value.length === 16 ? ':00' : '');
}

export function splitCsv(value) {
  if (!value) return [];
  return String(value).split(',').map((s) => s.trim()).filter(Boolean);
}

export function sum(list, pick = (x) => x) {
  return list.reduce((acc, item) => acc + (Number(pick(item)) || 0), 0);
}

export function debounce(fn, delay = 300) {
  let timer = null;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), delay);
  };
}

export function uid(prefix = 'id') {
  return `${prefix}_${Math.random().toString(36).slice(2, 9)}`;
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'download';
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function setLoading(visible, text = '加载中…') {
  const el = $('#loading');
  if (!el) return;
  el.hidden = !visible;
  const label = $('#loading-text');
  if (label) label.textContent = text;
}

export function sleep(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }

export function pickFile(accept = 'image/*') {
  return new Promise((resolve) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = accept;
    input.style.display = 'none';
    input.addEventListener('change', () => {
      resolve(input.files && input.files[0] ? input.files[0] : null);
      input.remove();
    });
    document.body.appendChild(input);
    input.click();
  });
}

export function statusTone(status) {
  const map = {
    0: 'danger', 1: 'success',
    2: 'warn', 3: 'info', 4: 'brand', 5: 'success', 6: 'danger', 7: 'warn'
  };
  return map[status] || 'default';
}

export function fileSize(bytes) {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0; let n = bytes;
  while (n >= 1024 && i < units.length - 1) { n /= 1024; i += 1; }
  return `${n.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

export function isEmpty(value) {
  return value === null || value === undefined || String(value).trim() === '';
}

export function html(strings, ...values) {
  return strings.reduce((out, str, i) => out + str + (values[i] ?? ''), '');
}
