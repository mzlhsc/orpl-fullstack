/* 菜品管理 */
import { api } from '../api.js';
import { $, escapeHtml, fmtDate, money, number, pickFile, fileSize } from '../util.js';
import {
  bindPager, confirmDialog, emptyTable, openModal, pagerHtml, setBtnLoading,
  statusTag, thumb, toast, toastError
} from '../ui.js';

const state = { page: 1, pageSize: 10, name: '', categoryId: '', status: '', total: 0, categories: [], selected: new Set() };

function categoryOptions(selected, includeAll = false) {
  return `${includeAll ? '<option value="">全部分类</option>' : '<option value="">请选择分类</option>'}`
    + state.categories.map((c) => `<option value="${c.id}" ${String(c.id) === String(selected) ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
}

async function loadCategories() {
  try { state.categories = await api.categoryList(1) || []; } catch (error) { state.categories = []; toastError(error); }
}

function paintTable(pageData) {
  const holder = $('#dish-table');
  const list = (pageData && pageData.list) || [];
  state.selected.clear();
  const rows = list.length ? list.map((d) => `
    <tr>
      <td style="width:36px"><input type="checkbox" class="checkbox" data-check="${d.id}" ${state.selected.has(d.id) ? 'checked' : ''}></td>
      <td><div class="cell-main">${thumb(d.image, d.name)}<div style="min-width:0"><div class="cell-strong ellipsis">${escapeHtml(d.name || '')}</div><div class="text-sm muted ellipsis" style="max-width:230px">${escapeHtml(d.description || '—')}</div></div></div></td>
      <td>${escapeHtml(d.categoryName || '—')}</td>
      <td class="num cell-strong">${money(d.price)}</td>
      <td><button class="link-btn" data-stock="${d.id}" data-name="${escapeHtml(d.name)}" title="点击设置库存">${d.stock == null ? '未设置' : `剩 ${number(d.stock)}`}</button></td>
      <td><button class="switch ${d.status === 1 ? 'on' : ''}" data-status="${d.id}" data-current="${d.status}" type="button" aria-label="起售停售"></button></td>
      <td class="text-sm muted">${escapeHtml(fmtDate(d.updateTime))}</td>
      <td><div class="row-actions">
        <button class="btn btn-ghost btn-xs" data-edit="${d.id}">编辑</button>
        <button class="btn btn-danger-ghost btn-xs" data-delete="${d.id}">删除</button>
      </div></td>
    </tr>`).join('') : emptyTable(8, '没有符合条件的菜品', '可新增菜品或调整筛选条件');

  const pager = pagerHtml(pageData ? pageData.total : 0, state.page, state.pageSize);
  holder.innerHTML = `
    ${state.selected.size ? '' : ''}
    <div class="table-wrap"><table class="data-table">
      <thead><tr>
        <th><input type="checkbox" class="checkbox" id="check-all"></th>
        <th style="min-width:260px">菜品</th><th style="width:110px">分类</th><th style="width:96px">价格</th>
        <th style="width:90px">库存</th><th style="width:70px">起售</th><th style="width:140px">更新时间</th><th style="width:130px">操作</th>
      </tr></thead><tbody>${rows}</tbody>
    </table></div>${pager.html}`;

  const all = holder.querySelector('#check-all');
  if (all) {
    all.addEventListener('change', () => {
      state.selected.clear();
      holder.querySelectorAll('[data-check]').forEach((cb) => {
        cb.checked = all.checked;
        if (all.checked) state.selected.add(Number(cb.dataset.check));
      });
      paintBatchBar();
    });
  }
  bindPager(holder, (page) => { state.page = page; loadList(); });
}

function paintBatchBar() {
  const bar = $('#dish-batch');
  if (!bar) return;
  if (!state.selected.size) { bar.hidden = true; bar.innerHTML = ''; return; }
  bar.hidden = false;
  bar.innerHTML = `<span>已选择 <strong>${state.selected.size}</strong> 个菜品</span>
    <div class="flex gap-8"><button class="btn btn-ghost btn-xs" id="dish-batch-clear" type="button">取消选择</button>
    <button class="btn btn-danger btn-xs" id="dish-batch-delete" type="button">批量删除</button></div>`;
  $('#dish-batch-clear').onclick = () => {
    state.selected.clear();
    document.querySelectorAll('[data-check]').forEach((cb) => { cb.checked = false; });
    const all = document.querySelector('#check-all'); if (all) all.checked = false;
    paintBatchBar();
  };
  $('#dish-batch-delete').onclick = async () => {
    if (!state.selected.size) return;
    const ok = await confirmDialog({ title: '批量删除菜品', message: `确定删除选中的 <strong>${state.selected.size}</strong> 个菜品吗？该操作不可撤销。`, danger: true, confirmText: '删除' });
    if (!ok) return;
    try {
      await api.dishDelete(Array.from(state.selected));
      toast('菜品已删除', 'success');
      state.selected.clear();
      await refresh();
    } catch (error) { toastError(error); }
  };
}

async function loadList() {
  const holder = $('#dish-table');
  holder.innerHTML = '<div class="table-empty">加载中…</div>';
  try {
    const data = await api.dishPage({
      page: state.page, pageSize: state.pageSize,
      name: state.name,
      categoryId: state.categoryId || undefined,
      status: state.status === '' ? undefined : Number(state.status)
    });
    state.total = data ? data.total : 0;
    paintTable(data);
    paintBatchBar();
  } catch (error) {
    toastError(error);
    holder.innerHTML = '<div class="table-empty">菜品加载失败</div>';
  }
}

function flavorRow(name = '', value = '') {
  return `<div class="flavor-row" data-flavor-row style="display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1.6fr) 34px;gap:8px;margin-bottom:8px">
    <input class="input" data-flavor-name placeholder="口味名，如甜度" value="${escapeHtml(name)}">
    <input class="input" data-flavor-value placeholder="口味值，英文逗号分隔，如：无糖,少糖" value="${escapeHtml(value)}">
    <button class="icon-btn" type="button" data-remove-flavor style="color:var(--danger)" title="删除该口味">
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 6l12 12M18 6L6 18"/></svg>
    </button>
  </div>`;
}

function openDishForm(dish) {
  const isEdit = !!dish;
  const image = dish ? dish.image : '';
  const flavors = dish && Array.isArray(dish.flavors) ? dish.flavors : [];
  openModal({
    title: isEdit ? '编辑菜品' : '新增菜品',
    subtitle: isEdit ? `菜品 ID ${dish.id}` : '带 * 的字段为必填项',
    size: 'modal-lg',
    body: `<form id="dish-form" class="form-grid" autocomplete="off">
      <div class="form-item"><label>菜品名称<em>*</em></label><input class="input" name="name" maxlength="32" value="${escapeHtml(dish?.name || '')}" placeholder="例如：宫保鸡丁"></div>
      <div class="form-item"><label>菜品分类<em>*</em></label><select class="select" name="categoryId">${categoryOptions(dish?.categoryId || '')}</select></div>
      <div class="form-item"><label>价格（元）<em>*</em></label><input class="input" name="price" type="number" step="0.01" min="0" value="${dish?.price ?? ''}" placeholder="0.00"></div>
      <div class="form-item"><label>库存</label><input class="input" name="stock" type="number" min="0" step="1" value="${dish?.stock ?? 100}" placeholder="默认 100">
        <span class="form-hint">库存为 0 时用户端不可下单，可随时在列表里调整</span></div>
      <div class="form-item span-2"><label>菜品图片</label>
        <div style="display:flex;gap:12px;align-items:flex-start">
          <div id="dish-img-preview" style="width:92px;height:92px;border-radius:10px;border:1px dashed var(--line);display:grid;place-items:center;overflow:hidden;background:#fafbfc">
            ${image ? `<img src="${escapeHtml(image)}" style="width:100%;height:100%;object-fit:cover" onerror="this.style.display='none'">` : '<span class="text-sm muted">暂无图片</span>'}
          </div>
          <div style="flex:1;display:grid;gap:8px">
            <input class="input" name="image" value="${escapeHtml(image)}" placeholder="图片地址，或点击下方按钮上传图片">
            <div class="flex gap-8"><button class="btn btn-ghost btn-sm" type="button" id="dish-upload">上传图片</button>
            <button class="btn btn-ghost btn-sm" type="button" id="dish-clear-image">清空</button></div>
            <span class="form-hint">上传接口：<code>POST /api/common/upload</code>（配了阿里云 OSS 走 OSS，没配则存服务器本地）</span>
          </div>
        </div>
      </div>
      <div class="form-item span-2"><label>菜品描述</label><textarea class="textarea" name="description" maxlength="200" placeholder="口味、食材、卖点等">${escapeHtml(dish?.description || '')}</textarea></div>
      <div class="form-item"><label>起售状态</label><select class="select" name="status">
        <option value="1" ${dish?.status !== 0 ? 'selected' : ''}>起售</option><option value="0" ${dish?.status === 0 ? 'selected' : ''}>停售</option>
      </select></div>
      <div class="form-item span-2"><label>口味配置</label>
        <div id="flavor-list">${flavors.map((f) => flavorRow(f.name || '', f.value || '')).join('')}</div>
        <div><button class="btn btn-ghost btn-sm" type="button" id="add-flavor">+ 添加口味</button></div>
        <span class="form-hint">口味值支持英文逗号分隔，例如「无糖,少糖,多糖」；提交给后端后会存为逗号分隔字符串</span>
      </div>
      <p class="form-error span-2" data-form-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button>
             <button class="btn btn-primary" type="button" data-save>${isEdit ? '保存修改' : '创建菜品'}</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#dish-form');
      const errorEl = modal.querySelector('[data-form-error]');
      const saveBtn = modal.querySelector('[data-save]');
      const imageInput = form.querySelector('[name="image"]');
      const preview = modal.querySelector('#dish-img-preview');
      const flavorList = modal.querySelector('#flavor-list');

      const setPreview = (url) => {
        if (url) preview.innerHTML = `<img src="${escapeHtml(url)}" style="width:100%;height:100%;object-fit:cover" onerror="this.style.display='none'">`;
        else preview.innerHTML = '<span class="text-sm muted">暂无图片</span>';
      };
      imageInput.addEventListener('input', () => setPreview(imageInput.value.trim()));
      modal.querySelector('#dish-clear-image').addEventListener('click', () => { imageInput.value = ''; setPreview(''); });
      modal.querySelector('#dish-upload').addEventListener('click', async (event) => {
        const btn = event.currentTarget;
        const file = await pickFile('image/*');
        if (!file) return;
        if (file.size > 10 * 1024 * 1024) { toast(`图片不能超过 10MB（当前 ${fileSize(file.size)}）`, 'error'); return; }
        setBtnLoading(btn, true, '上传中…');
        try {
          const url = await api.upload(file);
          if (typeof url === 'string' && url) {
            imageInput.value = url;
            setPreview(url);
            toast('图片上传成功', 'success');
          } else {
            throw new Error('上传接口未返回图片地址');
          }
        } catch (error) {
          toastError(error);
          toast('也可以直接粘贴可访问的图片 URL', 'info', 4000);
        } finally {
          setBtnLoading(btn, false);
        }
      });

      modal.querySelector('#add-flavor').addEventListener('click', () => {
        flavorList.insertAdjacentHTML('beforeend', flavorRow());
      });
      flavorList.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-remove-flavor]');
        if (btn) btn.closest('[data-flavor-row]').remove();
      });

      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const name = form.elements.namedItem('name').value.trim();
        const categoryId = Number(form.categoryId.value);
        const price = Number(form.price.value);
        const stock = Number(form.stock.value);
        if (!name) { errorEl.hidden = false; errorEl.textContent = '请填写菜品名称'; return; }
        if (!categoryId) { errorEl.hidden = false; errorEl.textContent = '请选择菜品分类'; return; }
        if (!Number.isFinite(price) || price < 0) { errorEl.hidden = false; errorEl.textContent = '请填写正确的价格'; return; }
        if (!Number.isFinite(stock) || stock < 0) { errorEl.hidden = false; errorEl.textContent = '库存不能为负数'; return; }
        const flavorsData = Array.from(flavorList.querySelectorAll('[data-flavor-row]')).map((row) => ({
          name: row.querySelector('[data-flavor-name]').value.trim(),
          value: row.querySelector('[data-flavor-value]').value.trim()
        })).filter((f) => f.name || f.value);
        const payload = {
          name, categoryId, price, stock,
          image: imageInput.value.trim(),
          description: form.description.value.trim(),
          status: Number(form.status.value),
          flavors: flavorsData
        };
        if (isEdit) payload.id = dish.id;
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          if (isEdit) await api.dishUpdate(payload); else await api.dishSave(payload);
          toast(isEdit ? '菜品已更新' : '菜品已创建', 'success');
          close();
          await loadList();
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message || '保存失败';
        } finally {
          setBtnLoading(saveBtn, false);
        }
      });
    }
  });
}

async function editDish(id) {
  try {
    const dish = await api.dishGet(id);
    if (!dish) throw new Error('菜品不存在');
    openDishForm(dish);
  } catch (error) { toastError(error); }
}

function openStock(id, name) {
  openModal({
    title: '设置库存',
    subtitle: `${name || `菜品 #${id}`} · 库存会直接影响用户端下单`,
    size: 'modal-sm',
    body: `<div class="form-item"><label>剩余库存<em>*</em></label><input class="input" type="number" min="0" step="1" id="stock-input" value=""><span class="form-hint">读取中…</span><p class="form-error" id="stock-error" hidden></p></div>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>保存库存</button>`,
    onMount: async (modal, close) => {
      const input = modal.querySelector('#stock-input');
      const hint = modal.querySelector('.form-hint');
      const errorEl = modal.querySelector('#stock-error');
      const saveBtn = modal.querySelector('[data-save]');
      try {
        const stock = await api.dishStock(id);
        input.value = stock == null ? 0 : stock;
        hint.textContent = '当前库存已读取，可直接修改';
      } catch (error) {
        hint.textContent = '库存读取失败，可直接填写';
      }
      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const stock = Number(input.value);
        if (!Number.isFinite(stock) || stock < 0) { errorEl.hidden = false; errorEl.textContent = '库存必须是非负整数'; return; }
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          await api.dishStockSet({ dishId: id, stock });
          toast('库存已更新', 'success');
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

async function toggleStatus(id, current) {
  const next = Number(current) === 1 ? 0 : 1;
  try {
    await api.dishStatus(next, id);
    toast(next === 1 ? '菜品已起售' : '菜品已停售', 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

async function deleteDish(id) {
  const ok = await confirmDialog({ title: '删除菜品', message: `确定删除菜品 <strong>#${id}</strong> 吗？该操作不可撤销。`, danger: true, confirmText: '删除' });
  if (!ok) return;
  try {
    await api.dishDelete([id]);
    toast('菜品已删除', 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

async function refresh() {
  await loadCategories();
  const filterCat = $('#f-category');
  if (filterCat) filterCat.innerHTML = categoryOptions(state.categoryId, true);
  await loadList();
}

export async function render(root) {
  Object.assign(state, { page: 1, pageSize: 10, name: '', categoryId: '', status: '', total: 0 });
  root.innerHTML = `
    <div class="section-stack">
      <div class="card">
        <div class="toolbar">
          <div class="toolbar-left">
            <input id="f-name" class="input filter-input" placeholder="菜品名称" autocomplete="off">
            <select id="f-category" class="select filter-select"><option value="">全部分类</option></select>
            <select id="f-status" class="select filter-select">
              <option value="">全部状态</option><option value="1">起售</option><option value="0">停售</option>
            </select>
            <button id="btn-search" class="btn btn-primary" type="button">查询</button>
            <button id="btn-reset" class="btn btn-ghost" type="button">重置</button>
          </div>
          <div class="toolbar-right">
            <button id="btn-add" class="btn btn-primary" type="button">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>新增菜品
            </button>
          </div>
        </div>
        <div id="dish-batch" class="batch-bar" hidden></div>
        <div id="dish-table"><div class="table-empty">加载中…</div></div>
      </div>
    </div>`;

  $('#btn-search').addEventListener('click', () => {
    state.page = 1;
    state.name = $('#f-name').value.trim();
    state.categoryId = $('#f-category').value;
    state.status = $('#f-status').value;
    loadList();
  });
  $('#btn-reset').addEventListener('click', () => {
    $('#f-name').value = ''; $('#f-category').value = ''; $('#f-status').value = '';
    state.page = 1; state.name = ''; state.categoryId = ''; state.status = '';
    loadList();
  });
  $('#f-name').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('#btn-search').click(); });
  $('#btn-add').addEventListener('click', () => {
    if (!state.categories.length) { toast('请先创建菜品分类', 'warn'); return; }
    openDishForm(null);
  });
  $('#dish-table').addEventListener('click', (event) => {
    const edit = event.target.closest('[data-edit]');
    const del = event.target.closest('[data-delete]');
    const stock = event.target.closest('[data-stock]');
    const status = event.target.closest('[data-status]');
    const check = event.target.closest('[data-check]');
    if (edit) { editDish(Number(edit.dataset.edit)); return; }
    if (del) { deleteDish(Number(del.dataset.delete)); return; }
    if (stock) { openStock(Number(stock.dataset.stock), stock.dataset.name); return; }
    if (status) { toggleStatus(Number(status.dataset.status), status.dataset.current); return; }
    if (check) {
      const id = Number(check.dataset.check);
      if (check.checked) state.selected.add(id); else state.selected.delete(id);
      paintBatchBar();
    }
  });

  await refresh();
}

export function destroy() {}


