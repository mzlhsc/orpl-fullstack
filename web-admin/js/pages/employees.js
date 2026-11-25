/* 员工管理 */
import { api, auth } from '../api.js';
import { $, escapeHtml, fmtDate } from '../util.js';
import { bindPager, confirmDialog, emptyTable, openModal, pagerHtml, setBtnLoading, statusTag, toast, toastError } from '../ui.js';

const state = { page: 1, pageSize: 10, name: '', total: 0 };

function renderTable(data) {
  const holder = $('#employee-table');
  const list = (data && data.list) || [];
  const me = auth.user || {};
  const rows = list.length ? list.map((e) => `
    <tr>
      <td><div class="cell-strong">${escapeHtml(e.name || '')}</div><div class="text-sm muted">${escapeHtml(e.idNumber || '')}</div></td>
      <td class="mono">${escapeHtml(e.username || '')}</td>
      <td>${escapeHtml(e.phone || '—')}</td>
      <td>${escapeHtml(e.sex === '1' ? '男' : e.sex === '2' ? '女' : (e.sex || '—'))}</td>
      <td><button class="switch ${e.status === 1 ? 'on' : ''}" data-status="${e.id}" data-current="${e.status}" type="button" aria-label="启用禁用"></button></td>
      <td class="text-sm muted">${escapeHtml(fmtDate(e.createTime))}</td>
      <td><div class="row-actions">
        <button class="btn btn-ghost btn-xs" data-edit="${e.id}">编辑</button>
        ${Number(e.id) === Number(me.id) ? '<span class="tag tag-info">当前账号</span>' : ''}
      </div></td>
    </tr>`).join('') : emptyTable(7, '还没有员工', '点击右上角「新增员工」创建账号');
  const pager = pagerHtml(data ? data.total : 0, state.page, state.pageSize);
  holder.innerHTML = `<div class="table-wrap"><table class="data-table">
    <thead><tr><th style="min-width:170px">姓名 / 身份证</th><th style="width:120px">用户名</th><th style="width:130px">手机号</th><th style="width:70px">性别</th><th style="width:80px">状态</th><th style="width:150px">创建时间</th><th style="width:120px">操作</th></tr></thead>
    <tbody>${rows}</tbody></table></div>${pager.html}`;
  bindPager(holder, (page) => { state.page = page; loadList(); });
}

async function loadList() {
  const holder = $('#employee-table');
  holder.innerHTML = '<div class="table-empty">加载中…</div>';
  try {
    const data = await api.employeePage({ page: state.page, pageSize: state.pageSize, name: state.name });
    state.total = data ? data.total : 0;
    renderTable(data);
  } catch (error) {
    toastError(error);
    holder.innerHTML = '<div class="table-empty">员工加载失败</div>';
  }
}

function openForm(employee) {
  const isEdit = !!employee;
  openModal({
    title: isEdit ? '编辑员工' : '新增员工',
    subtitle: isEdit ? '编辑不会修改密码与状态' : '新增员工默认密码 123456，状态为启用',
    size: 'modal-lg',
    body: `<form id="emp-form" class="form-grid" autocomplete="off">
      <div class="form-item"><label>姓名<em>*</em></label><input class="input" name="name" maxlength="20" value="${escapeHtml(employee?.name || '')}" placeholder="员工姓名"></div>
      <div class="form-item"><label>用户名<em>*</em></label><input class="input" name="username" maxlength="32" value="${escapeHtml(employee?.username || '')}" placeholder="登录用户名，需唯一"></div>
      <div class="form-item"><label>手机号<em>*</em></label><input class="input" name="phone" maxlength="20" value="${escapeHtml(employee?.phone || '')}" placeholder="11 位手机号"></div>
      <div class="form-item"><label>性别<em>*</em></label>
        <select class="select" name="sex">
          <option value="">请选择</option>
          <option value="1" ${employee?.sex === '1' || employee?.sex === '男' ? 'selected' : ''}>男</option>
          <option value="2" ${employee?.sex === '2' || employee?.sex === '女' ? 'selected' : ''}>女</option>
        </select>
      </div>
      <div class="form-item span-2"><label>身份证号<em>*</em></label><input class="input" name="idNumber" maxlength="18" value="${escapeHtml(employee?.idNumber || '')}" placeholder="18 位身份证号"></div>
      ${isEdit ? '<p class="form-hint span-2">提示：密码与启用状态请分别在「店铺设置 → 修改密码」和列表开关中调整。</p>' : ''}
      <p class="form-error span-2" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button>
             <button class="btn btn-primary" type="button" data-save>${isEdit ? '保存修改' : '创建员工'}</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#emp-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const name = form.elements.namedItem('name').value.trim();
        const username = form.username.value.trim();
        const phone = form.phone.value.trim();
        const sex = form.sex.value;
        const idNumber = form.idNumber.value.trim();
        if (!name || !username || !phone || !sex || !idNumber) {
          errorEl.hidden = false; errorEl.textContent = '姓名、用户名、手机号、性别和身份证号不能为空'; return;
        }
        if (!/^1\d{10}$/.test(phone)) { errorEl.hidden = false; errorEl.textContent = '手机号格式不正确'; return; }
        const payload = { name, username, phone, sex, idNumber };
        if (isEdit) payload.id = employee.id;
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          if (isEdit) await api.employeeUpdate(payload); else await api.employeeSave(payload);
          toast(isEdit ? '员工信息已更新' : '员工已创建，默认密码为 123456', 'success');
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

async function editEmployee(id) {
  try {
    const employee = await api.employeeGet(id);
    if (!employee) throw new Error('员工不存在');
    openForm(employee);
  } catch (error) { toastError(error); }
}

async function toggleStatus(id, current, name) {
  const next = Number(current) === 1 ? 0 : 1;
  const action = next === 1 ? '启用' : '禁用';
  const ok = await confirmDialog({
    title: `${action}员工`,
    message: `确定${action}员工 <strong>${escapeHtml(name || id)}</strong> 吗？${next === 0 ? '禁用后该账号将无法登录。' : ''}`,
    danger: next === 0,
    confirmText: action
  });
  if (!ok) return;
  try {
    await api.employeeStatus(next, id);
    toast(`已${action}该员工`, 'success');
    await loadList();
  } catch (error) { toastError(error); }
}

export async function render(root) {
  Object.assign(state, { page: 1, pageSize: 10, name: '', total: 0 });
  root.innerHTML = `
    <div class="section-stack">
      <div class="card">
        <div class="toolbar">
          <div class="toolbar-left">
            <input id="f-name" class="input filter-input" placeholder="员工姓名" autocomplete="off">
            <button id="btn-search" class="btn btn-primary" type="button">查询</button>
            <button id="btn-reset" class="btn btn-ghost" type="button">重置</button>
          </div>
          <div class="toolbar-right">
            <button id="btn-add" class="btn btn-primary" type="button">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>新增员工
            </button>
          </div>
        </div>
        <div id="employee-table"><div class="table-empty">加载中…</div></div>
      </div>
    </div>`;

  $('#btn-search').addEventListener('click', () => {
    state.page = 1;
    state.name = $('#f-name').value.trim();
    loadList();
  });
  $('#btn-reset').addEventListener('click', () => {
    $('#f-name').value = '';
    state.page = 1; state.name = '';
    loadList();
  });
  $('#f-name').addEventListener('keydown', (e) => { if (e.key === 'Enter') $('#btn-search').click(); });
  $('#btn-add').addEventListener('click', () => openForm(null));
  $('#employee-table').addEventListener('click', (event) => {
    const edit = event.target.closest('[data-edit]');
    const status = event.target.closest('[data-status]');
    if (edit) {
      const id = Number(edit.dataset.edit);
      editEmployee(id);
      return;
    }
    if (status) {
      const row = status.closest('tr');
      const name = row ? row.querySelector('.cell-strong')?.textContent : '';
      toggleStatus(Number(status.dataset.status), status.dataset.current, name);
    }
  });

  await loadList();
}

export function destroy() {}

