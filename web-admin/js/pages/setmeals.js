/* 套餐管理 */
import { api } from '../api.js';
import { $, escapeHtml, fileSize, fmtDate, money, number, pickFile } from '../util.js';
import {
  bindPager, confirmDialog, emptyTable, openModal, pagerHtml, setBtnLoading,
  thumb, toast, toastError
} from '../ui.js';

const state = {
  page: 1, pageSize: 10, name: '', categoryId: '', status: '', total: 0,
  categories: [], dishCategories: [], dishes: [], selected: new Set()
};

function catOptions(selected, all = false) {
  return `${all ? '<option value="">全部分类</option>' : '<option value="">请选择分类</option>'}`
    + state.categories.map((c) => `<option value="${c.id}" ${String(c.id) === String(selected) ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
}

async function loadDictionaries() {
  try {
    const [setmealCats, dishCats] = await Promise.all([api.categoryList(2), api.categoryList(1)]);
    state.categories = setmealCats || [];
    state.dishCategories = dishCats || [];
    const lists = await Promise.all(state.dishCategories.map((c) => api.dishList(c.id).catch(() => [])));
    state.dishes = lists.flat().filter(Boolean).map((d) => ({
      ...d,
      categoryName: (state.dishCategories.find((c) => c.id === d.categoryId) || {}).name || '未分类'
    }));
  } catch (error) {
    state.categories = []; state.dishCategories = []; state.dishes = [];
    toastError(error);
  }
}

function renderTable(pageData) {
  const holder = $('#setmeal-table');
  const list = (pageData && pageData.list) || [];
  state.selected.clear();
  const rows = list.length ? list.map((s) => {
    return `<tr>
      <td style="width:36px"><input type="checkbox" class="checkbox" data-check="${s.id}"></td>
      <td><div class="cell-main">${thumb(s.image, s.name)}<div style="min-width:0"><div class="cell-strong ellipsis">${escapeHtml(s.name || '')}</div><div class="text-sm muted ellipsis" style="max-width:230px">${escapeHtml(s.description || '—')}</div></div></div></td>
      <td>${escapeHtml(s.categoryName || '—')}</td>
      <td class="num cell-strong">${money(s.price)}</td>
      <td><button class="switch ${s.status === 1 ? 'on' : ''}" data-status="${s.id}" data-current="${s.status}" type="button" aria-label="起售停售"></button></td>
      <td class="text-sm muted">${escapeHtml(fmtDate(s.updateTime))}</td>
      <td><div class="row-actions">
        <button class="btn btn-ghost btn-xs" data-edit="${s.id}">编辑</button>
        <button class="btn btn-danger-ghost btn-xs" data-delete="${s.id}">删除</button>
      </div></td>
    </tr>`;
  }).join('') : emptyTable(7, '没有符合条件的套餐', '可新增套餐或调整筛选条件');
  const pager = pagerHtml(pageData ? pageData.total : 0, state.page, state.pageSize);
  holder.innerHTML = `<div class="table-wrap"><table class="data-table">
    <thead><tr><th><input type="checkbox" class="checkbox" id="sm-check-all"></th>
    <th style="min-width:260px">套餐</th><th style="width:120px">分类</th><th style="width:96px">价格</th>
    <th style="width:70px">起售</th><th style="width:140px">更新时间</th><th style="width:130px">操作</th></tr></thead>
    <tbody>${rows}</tbody></table></div>${pager.html}`;
  const all = holder.querySelector('#sm-check-all');
  all.addEventListener('change', () => {
    state.selected.clear();
    holder.querySelectorAll('[data-check]').forEach((cb) => {
      cb.checked = all.checked;
      if (all.checked) state.selected.add(Number(cb.dataset.check));
    });
    paintBatchBar();
  });
  bindPager(holder, (page) => { state.page = page; loadList(); });
}

function paintBatchBar() {
  const bar = $('#setmeal-batch');
  if (!bar) return;
  if (!state.selected.size) { bar.hidden = true; bar.innerHTML = ''; return; }
  bar.hidden = false;
  bar.innerHTML = `<span>已选择 <strong>${state.selected.size}</strong> 个套餐</span>
    <div class="flex gap-8"><button class="btn btn-ghost btn-xs" id="sm-batch-clear" type="button">取消选择</button>
    <button class="btn btn-danger btn-xs" id="sm-batch-delete" type="button">批量删除</button></div>`;
  $('#sm-batch-clear').onclick = () => {
    state.selected.clear();
    document.querySelectorAll('[data-check]').forEach((cb) => { cb.checked = false; });
    const all = document.querySelector('#sm-check-all'); if (all) all.checked = false;
    paintBatchBar();
  };
  $('#sm-batch-delete').onclick = async () => {
    const ok = await confirmDialog({ title: '批量删除套餐', message: `确定删除选中的 <strong>${state.selected.size}</strong> 个套餐吗？`, danger: true, confirmText: '删除' });
    if (!ok) return;
    try {
      await api.setmealDelete(Array.from(state.selected));
      toast('套餐已删除', 'success');
      state.selected.clear();
      await loadList();
    } catch (error) { toastError(error); }
  };
}

async function loadList() {
  const holder = $('#setmeal-table');
  holder.innerHTML = '<div class="table-empty">加载中…</div>';
  try {
    const data = await api.setmealPage({
      page: state.page, pageSize: state.pageSize,
      name: state.name,
      categoryId: state.categoryId || undefined,
      status: state.status === '' ? undefined : Number(state.status)
    });
    renderTable(data);
    paintBatchBar();
  } catch (error) {
    toastError(error);
    holder.innerHTML = '<div class="table-empty">套餐加载失败</div>';
  }
}

function openSetmealForm(setmeal) {
  const isEdit = !!setmeal;
  const image = setmeal ? setmeal.image : '';
  const existing = Array.isArray(setmeal?.setmealDishes) ? setmeal.setmealDishes : [];
  const selected = new Map(existing.map((d) => [Number(d.dishId), { ...d, copies: d.copies || 1 }]));
  const dishOptions = () => state.dishes.map((d) => {
    const checked = selected.has(Number(d.id));
    return `<label class="dish-option" style="display:flex;align-items:center;gap:9px;padding:8px 9px;border:1px solid var(--line-soft);border-radius:8px;cursor:pointer;background:${checked ? '#fff7f3' : '#fff'}">
      <input type="checkbox" data-dish-pick="${d.id}" ${checked ? 'checked' : ''}>
      <span style="flex:1;min-width:0"><span style="display:block;font-size:12.5px;font-weight:500">${escapeHtml(d.name)}</span><span class="text-sm muted">${escapeHtml(d.categoryName)} · ${money(d.price)}</span></span>
    </label>`;
  }).join('');

  openModal({
    title: isEdit ? '编辑套餐' : '新增套餐',
    subtitle: isEdit ? `套餐 ID ${setmeal.id}` : '套餐分类来自「套餐分类」配置',
    size: 'modal-xl',
    body: `<form id="sm-form" class="form-grid" autocomplete="off">
      <div class="form-item"><label>套餐名称<em>*</em></label><input class="input" name="name" maxlength="32" value="${escapeHtml(setmeal?.name || '')}" placeholder="例如：双人欢聚餐"></div>
      <div class="form-item"><label>套餐分类<em>*</em></label><select class="select" name="categoryId">${catOptions(setmeal?.categoryId || '')}</select></div>
      <div class="form-item"><label>套餐价格（元）<em>*</em></label><input class="input" name="price" type="number" step="0.01" min="0" value="${setmeal?.price ?? ''}" placeholder="0.00"></div>
      <div class="form-item"><label>起售状态</label><select class="select" name="status">
        <option value="1" ${setmeal?.status !== 0 ? 'selected' : ''}>起售</option><option value="0" ${setmeal?.status === 0 ? 'selected' : ''}>停售</option>
      </select></div>
      <div class="form-item span-2"><label>套餐图片</label>
        <div style="display:flex;gap:12px;align-items:flex-start">
          <div id="sm-img-preview" style="width:92px;height:92px;border-radius:10px;border:1px dashed var(--line);display:grid;place-items:center;overflow:hidden;background:#fafbfc">
            ${image ? `<img src="${escapeHtml(image)}" style="width:100%;height:100%;object-fit:cover" onerror="this.style.display='none'">` : '<span class="text-sm muted">暂无图片</span>'}
          </div>
          <div style="flex:1;display:grid;gap:8px">
            <input class="input" name="image" value="${escapeHtml(image)}" placeholder="图片地址，或点击上方按钮上传图片">
            <div class="flex gap-8"><button class="btn btn-ghost btn-sm" type="button" id="sm-upload">上传图片</button>
            <button class="btn btn-ghost btn-sm" type="button" id="sm-clear-image">清空</button></div>
          </div>
        </div>
      </div>
      <div class="form-item span-2"><label>套餐描述</label><textarea class="textarea" name="description" maxlength="200" placeholder="套餐适用人数、卖点等">${escapeHtml(setmeal?.description || '')}</textarea></div>
      <div class="form-item span-2"><label>套餐菜品<em>*</em></label>
        <div style="display:grid;grid-template-columns:minmax(0,1.2fr) minmax(0,.8fr);gap:12px">
          <div style="border:1px solid var(--line);border-radius:10px;overflow:hidden">
            <div style="padding:9px 10px;background:#fafbfc;border-bottom:1px solid var(--line-soft);font-size:12px;color:var(--muted);display:flex;justify-content:space-between"><span>可添加菜品</span><span>${state.dishes.length} 个</span></div>
            <div id="sm-dish-options" style="max-height:300px;overflow:auto;padding:9px;display:grid;gap:6px">${dishOptions() || '<div class="text-sm muted" style="padding:10px">暂无可选菜品</div>'}</div>
          </div>
          <div style="border:1px solid var(--line);border-radius:10px;overflow:hidden">
            <div style="padding:9px 10px;background:#fafbfc;border-bottom:1px solid var(--line-soft);font-size:12px;color:var(--muted);display:flex;justify-content:space-between"><span>已选菜品</span><span id="sm-selected-count">${selected.size} 个</span></div>
            <div id="sm-selected" style="max-height:300px;overflow:auto;padding:9px;display:grid;gap:6px"></div>
          </div>
        </div>
        <span class="form-hint">份数会写入 setmeal_dishes.copies，影响套餐内菜品数量</span>
      </div>
      <p class="form-error span-2" data-form-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button>
             <button class="btn btn-primary" type="button" data-save>${isEdit ? '保存修改' : '创建套餐'}</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#sm-form');
      const errorEl = modal.querySelector('[data-form-error]');
      const saveBtn = modal.querySelector('[data-save]');
      const imageInput = form.querySelector('[name="image"]');
      const preview = modal.querySelector('#sm-img-preview');
      const optionsBox = modal.querySelector('#sm-dish-options');
      const selectedBox = modal.querySelector('#sm-selected');
      const selectedCount = modal.querySelector('#sm-selected-count');

      const syncPickStyles = () => {
        optionsBox.querySelectorAll('[data-dish-pick]').forEach((cb) => {
          const label = cb.closest('label');
          if (label) label.style.background = cb.checked ? '#fff7f3' : '#fff';
        });
      };
      const paintSelected = () => {
        selectedCount.textContent = `${selected.size} 个`;
        selectedBox.innerHTML = selected.size ? Array.from(selected.values()).map((d) => `
          <div style="display:flex;align-items:center;gap:8px;padding:7px 8px;border:1px solid var(--line-soft);border-radius:8px" data-selected-row="${d.dishId}">
            <span style="flex:1;min-width:0"><span style="display:block;font-size:12.5px;font-weight:500">${escapeHtml(d.name || '')}</span><span class="text-sm muted">${money(d.price || 0)}</span></span>
            <span class="text-sm muted">份数</span>
            <input class="input" type="number" min="1" step="1" value="${d.copies}" data-copies="${d.dishId}" style="width:62px;height:30px;padding:0 7px;font-size:12px">
            <button class="icon-btn" type="button" data-remove-selected="${d.dishId}" style="color:var(--danger);width:28px;height:28px" title="移除">
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>`).join('') : '<div class="text-sm muted" style="padding:10px">尚未选择菜品</div>';
        syncPickStyles();
      };
      paintSelected();

      optionsBox.addEventListener('change', (event) => {
        const cb = event.target.closest('[data-dish-pick]');
        if (!cb) return;
        const id = Number(cb.dataset.dishPick);
        if (cb.checked) {
          const dish = state.dishes.find((d) => Number(d.id) === id);
          if (dish) selected.set(id, { dishId: id, name: dish.name, price: dish.price, copies: 1 });
        } else {
          selected.delete(id);
        }
        paintSelected();
      });
      selectedBox.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-remove-selected]');
        if (!btn) return;
        const id = Number(btn.dataset.removeSelected);
        selected.delete(id);
        const cb = optionsBox.querySelector(`[data-dish-pick="${id}"]`);
        if (cb) cb.checked = false;
        paintSelected();
      });
      selectedBox.addEventListener('change', (event) => {
        const input = event.target.closest('[data-copies]');
        if (!input) return;
        const id = Number(input.dataset.copies);
        const item = selected.get(id);
        if (item) item.copies = Math.max(1, Number(input.value) || 1);
      });

      const setPreview = (url) => {
        if (url) preview.innerHTML = `<img src="${escapeHtml(url)}" style="width:100%;height:100%;object-fit:cover" onerror="this.style.display='none'">`;
        else preview.innerHTML = '<span class="text-sm muted">暂无图片</span>';
      };
      imageInput.addEventListener('input', () => setPreview(imageInput.value.trim()));
      modal.querySelector('#sm-clear-image').addEventListener('click', () => { imageInput.value = ''; setPreview(''); });
      modal.querySelector('#sm-upload').addEventListener('click', async (event) => {
        const btn = event.currentTarget;
        const file = await pickFile('image/*');
        if (!file) return;
        if (file.size > 10 * 1024 * 1024) { toast(`图片不能超过 10MB（当前 ${fileSize(file.size)}）`, 'error'); return; }
        setBtnLoading(btn, true, '上传中…');
        try {
          const url = await api.upload(file);
          if (!url) throw new Error('上传接口未返回图片地址');
          imageInput.value = url;
          setPreview(url);
          toast('图片上传成功', 'success');
        } catch (error) {
          toastError(error);
        } finally { setBtnLoading(btn, false); }
      });

      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const name = form.elements.namedItem('name').value.trim();
        const categoryId = Number(form.categoryId.value);
        const price = Number(form.price.value);
        if (!name) { errorEl.hidden = false; errorEl.textContent = '请填写套餐名称'; return; }
        if (!categoryId) { errorEl.hidden = false; errorEl.textContent = '请选择套餐分类'; return; }
        if (!Number.isFinite(price) || price < 0) { errorEl.hidden = false; errorEl.textContent = '请填写正确的价格'; return; }
        if (!selected.size) { errorEl.hidden = false; errorEl.textContent = '请至少选择一个套餐菜品'; return; }
        const payload = {
          name, categoryId, price,
          image: imageInput.value.trim(),
          description: form.description.value.trim(),
          status: Number(form.status.value),
          setmealDishes: Array.from(selected.values()).map((d) => ({
            dishId: d.dishId, name: d.name, price: d.price, copies: Math.max(1, Number(d.copies) || 1)
          }))
        };
        if (isEdit) payload.id = setmeal.id;
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          if (isEdit) await api.setmealUpdate(payload); else await api.setmealSave(payload);
          toast(isEdit ? '套餐已更新' : '套餐已创建', 'success');
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

async function editSetmeal(id) {
  try {
    const vo = await api.setmealGet(id);
    if (!vo) throw new Error('套餐不存在');
    openSetmealForm(vo);
  } catch (error) { toastError(error); }
}

async function toggleStatus(id, current) {
  const next = Number(current) === 1 ? 0 : 1;
  try {
    await api.setmealStatus(next, id);
    toast(next === 1 ? '套餐已起售' : '套餐已停售', 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

async function deleteSetmeal(id) {
  const ok = await confirmDialog({ title: '删除套餐', message: `确定删除套餐 <strong>#${id}</strong> 吗？`, danger: true, confirmText: '删除' });
  if (!ok) return;
  try {
    await api.setmealDelete([id]);
    toast('套餐已删除', 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

export async function render(root) {
  Object.assign(state, { page: 1, pageSize: 10, name: '', categoryId: '', status: '', selected: new Set() });
  root.innerHTML = `
    <div class="section-stack">
      <div class="card">
        <div class="toolbar">
          <div class="toolbar-left">
            <input id="f-name" class="input filter-input" placeholder="套餐名称" autocomplete="off">
            <select id="f-category" class="select filter-select"><option value="">全部分类</option></select>
            <select id="f-status" class="select filter-select"><option value="">全部状态</option><option value="1">起售</option><option value="0">停售</option></select>
            <button id="btn-search" class="btn btn-primary" type="button">查询</button>
            <button id="btn-reset" class="btn btn-ghost" type="button">重置</button>
          </div>
          <div class="toolbar-right">
            <button id="btn-add" class="btn btn-primary" type="button">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>新增套餐
            </button>
          </div>
        </div>
        <div id="setmeal-batch" class="batch-bar" hidden></div>
        <div id="setmeal-table"><div class="table-empty">加载中…</div></div>
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
    if (!state.categories.length) { toast('请先在「分类管理」中创建套餐分类', 'warn'); return; }
    if (!state.dishes.length) { toast('暂无可选菜品，请先维护菜品', 'warn'); return; }
    openSetmealForm(null);
  });
  $('#setmeal-table').addEventListener('click', (event) => {
    const edit = event.target.closest('[data-edit]');
    const del = event.target.closest('[data-delete]');
    const status = event.target.closest('[data-status]');
    const check = event.target.closest('[data-check]');
    if (edit) { editSetmeal(Number(edit.dataset.edit)); return; }
    if (del) { deleteSetmeal(Number(del.dataset.delete)); return; }
    if (status) { toggleStatus(Number(status.dataset.status), status.dataset.current); return; }
    if (check) {
      const id = Number(check.dataset.check);
      if (check.checked) state.selected.add(id); else state.selected.delete(id);
      paintBatchBar();
    }
  });

  await loadDictionaries();
  $('#f-category').innerHTML = catOptions(state.categoryId, true);
  await loadList();
}

export function destroy() {}


