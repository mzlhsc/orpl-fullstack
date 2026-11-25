/* 堂食管理：桌台 / 预约 / 会话 / 报表 */
import { api } from '../api.js';
import { $, escapeHtml, fmtDate, fromDateTimeInput, money, number, percent, toDateTimeInput } from '../util.js';
import {
  bindPager, confirmDialog, emptyTable, openDrawer, openModal, promptDialog, setBtnLoading,
  statusTag, toast, toastError
} from '../ui.js';
import { barChart, lineChart } from '../charts.js';

const TABLE_STATUS = {
  0: { text: '空闲', tone: 'success', cls: 'is-free' },
  1: { text: '就餐中', tone: 'brand', cls: 'is-occupied' },
  2: { text: '待结账', tone: 'warn', cls: 'is-checkout' }
};
const SESSION_STATUS = {
  1: { text: '就餐中', tone: 'brand' },
  2: { text: '待结账', tone: 'warn' },
  3: { text: '已结账', tone: 'success' },
  4: { text: '已关闭', tone: 'danger' }
};
const RESERVE_STATUS = {
  1: { text: '待确认', tone: 'warn' },
  2: { text: '已确认', tone: 'info' },
  3: { text: '已到店', tone: 'success' },
  4: { text: '已取消', tone: 'danger' },
  5: { text: '已过期', tone: 'default' }
};

const state = {
  tab: 'tables',
  tables: [],
  reservations: [],
  sessions: [],
  filters: {
    tableStatus: '', area: '',
    reserveStatus: '', reserveDate: '',
    sessionStatus: '', sessionTable: ''
  }
};
let reportChart = null;

const tabsBar = () => `
  <div class="flex-between" style="flex-wrap:wrap;gap:10px">
    <div class="tabs" id="dining-tabs">
      <button class="tab ${state.tab === 'tables' ? 'active' : ''}" data-tab="tables" type="button">桌台</button>
      <button class="tab ${state.tab === 'reservations' ? 'active' : ''}" data-tab="reservations" type="button">预约</button>
      <button class="tab ${state.tab === 'sessions' ? 'active' : ''}" data-tab="sessions" type="button">就餐会话</button>
      <button class="tab ${state.tab === 'report' ? 'active' : ''}" data-tab="report" type="button">经营报表</button>
    </div>
    <div class="text-sm muted" id="dining-tab-hint">桌台状态实时同步，开台后自动占用桌台</div>
  </div>`;

async function loadTables() {
  try {
    state.tables = await api.diningTables({}) || [];
  } catch (error) {
    state.tables = [];
    toastError(error);
  }
  return state.tables;
}

function tableCard(t) {
  const info = TABLE_STATUS[t.status] || { text: `状态 ${t.status}`, tone: 'default', cls: '' };
  return `<div class="table-card ${info.cls}">
    <div class="table-card-head">
      <div>
        <div class="table-name">${escapeHtml(t.name || '')}</div>
        <div class="table-meta">${escapeHtml(t.area || '未分区')} · ${number(t.seats)} 座 ${t.reservationEnabled === 1 ? '· 可预约' : '· 不可预约'}</div>
      </div>
      <span class="table-status-tag">${statusTag(info.text, info.tone)}</span>
    </div>
    <div class="table-card-body">
      <div class="flex-between"><span>当前会话</span><strong>${t.currentSessionId ? `#${t.currentSessionId} · ${escapeHtml(t.currentSessionNo || '')}` : '无'}</strong></div>
      <div class="flex-between"><span>二维码令牌</span><strong class="mono" style="font-size:10.5px">${escapeHtml((t.qrToken || '').slice(0, 12))}${t.qrToken ? '…' : '—'}</strong></div>
    </div>
    <div class="table-card-actions">
      ${t.status === 0 ? `<button class="btn btn-primary btn-xs" data-open="${t.id}">开台</button>` : `<button class="btn btn-soft btn-xs" data-session="${t.currentSessionId}">查看会话</button>
      <button class="btn btn-danger-ghost btn-xs" data-close-session="${t.currentSessionId}">关闭会话</button>`}
      <button class="btn btn-ghost btn-xs" data-qr="${t.id}">二维码</button>
      <button class="btn btn-ghost btn-xs" data-edit="${t.id}">编辑</button>
      <button class="btn btn-danger-ghost btn-xs" data-del="${t.id}">删除</button>
    </div>
  </div>`;
}

async function renderTables(container) {
  const tables = await loadTables();
  const filtered = tables.filter((t) => (state.filters.tableStatus === '' || Number(t.status) === Number(state.filters.tableStatus))
    && (!state.filters.area || String(t.area || '').includes(state.filters.area)));
  const areas = Array.from(new Set(tables.map((t) => t.area).filter(Boolean)));
  container.innerHTML = `
    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <select id="t-status" class="select filter-select">
            <option value="">全部状态</option><option value="0" ${state.filters.tableStatus === '0' ? 'selected' : ''}>空闲</option>
            <option value="1" ${state.filters.tableStatus === '1' ? 'selected' : ''}>就餐中</option><option value="2" ${state.filters.tableStatus === '2' ? 'selected' : ''}>待结账</option>
          </select>
          <input id="t-area" class="input filter-input" placeholder="区域，如大厅" value="${escapeHtml(state.filters.area)}">
          <button id="t-search" class="btn btn-primary" type="button">查询</button>
          <button id="t-reset" class="btn btn-ghost" type="button">重置</button>
        </div>
        <div class="toolbar-right">
          <button id="t-add" class="btn btn-primary" type="button">
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 5v14M5 12h14"/></svg>新增桌台
          </button>
        </div>
      </div>
      <div class="card-body card-body-pad">
        <div class="flex gap-8" style="margin-bottom:12px;flex-wrap:wrap">
          <span class="tag tag-success"><span class="tag-dot"></span>空闲 ${tables.filter((t) => t.status === 0).length}</span>
          <span class="tag tag-brand"><span class="tag-dot"></span>就餐中 ${tables.filter((t) => t.status === 1).length}</span>
          <span class="tag tag-warn"><span class="tag-dot"></span>待结账 ${tables.filter((t) => t.status === 2).length}</span>
          <span class="text-sm muted">共 ${tables.length} 张桌台${areas.length ? ` · 区域：${escapeHtml(areas.join(' / '))}` : ''}</span>
        </div>
        ${filtered.length ? `<div class="table-grid">${filtered.map(tableCard).join('')}</div>` : `<div class="empty-block">
          <svg viewBox="0 0 64 64" width="50" height="50" fill="none" stroke="currentColor" stroke-width="2"><rect x="10" y="24" width="44" height="22" rx="5"/><path d="M16 24V16M48 24V16M20 46v6M44 46v6"/></svg>
          <strong>没有符合条件的桌台</strong><span>可调整筛选条件或新增桌台</span></div>`}
      </div>
    </div>`;

  $('#t-search').onclick = () => {
    state.filters.tableStatus = $('#t-status').value;
    state.filters.area = $('#t-area').value.trim();
    renderTables(container);
  };
  $('#t-reset').onclick = () => {
    state.filters.tableStatus = ''; state.filters.area = '';
    renderTables(container);
  };
  $('#t-add').onclick = () => openTableForm(null);

  container.querySelector('.card').addEventListener('click', (event) => {
    const open = event.target.closest('[data-open]');
    const edit = event.target.closest('[data-edit]');
    const qr = event.target.closest('[data-qr]');
    const del = event.target.closest('[data-del]');
    const session = event.target.closest('[data-session]');
    const closeSession = event.target.closest('[data-close-session]');
    if (open) { openOpenTable(Number(open.dataset.open)); return; }
    if (edit) { openTableForm(state.tables.find((t) => Number(t.id) === Number(edit.dataset.edit))); return; }
    if (qr) { showQr(state.tables.find((t) => Number(t.id) === Number(qr.dataset.qr))); return; }
    if (del) { deleteTable(state.tables.find((t) => Number(t.id) === Number(del.dataset.del))); return; }
    if (session) { showSession(Number(session.dataset.session)); return; }
    if (closeSession) { closeDiningSession(Number(closeSession.dataset.closeSession)); }
  });
}

function openTableForm(table) {
  const isEdit = !!table;
  openModal({
    title: isEdit ? '编辑桌台' : '新增桌台',
    subtitle: isEdit ? `桌台 ID ${table.id}` : '二维码令牌由后端自动生成',
    size: 'modal-sm',
    body: `<form id="table-form" class="form-grid" style="grid-template-columns:1fr" autocomplete="off">
      <div class="form-item"><label>桌台名称<em>*</em></label><input class="input" name="name" maxlength="16" value="${escapeHtml(table?.name || '')}" placeholder="例如 A01"></div>
      <div class="form-item"><label>座位数</label><input class="input" name="seats" type="number" min="1" step="1" value="${table?.seats ?? 4}"></div>
      <div class="form-item"><label>区域</label><input class="input" name="area" maxlength="16" value="${escapeHtml(table?.area || '')}" placeholder="例如 大厅 / 包间"></div>
      <div class="form-item"><label>允许预约</label><select class="select" name="reservationEnabled">
        <option value="1" ${table?.reservationEnabled !== 0 ? 'selected' : ''}>允许</option><option value="0" ${table?.reservationEnabled === 0 ? 'selected' : ''}>不允许</option>
      </select></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>${isEdit ? '保存修改' : '创建桌台'}</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#table-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const name = form.elements.namedItem('name').value.trim();
        const seats = Number(form.seats.value);
        if (!name) { errorEl.hidden = false; errorEl.textContent = '请填写桌台名称'; return; }
        if (!Number.isFinite(seats) || seats < 1) { errorEl.hidden = false; errorEl.textContent = '座位数至少为 1'; return; }
        const payload = { name, seats, area: form.area.value.trim(), reservationEnabled: Number(form.reservationEnabled.value) };
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          if (isEdit) await api.diningTableUpdate(table.id, payload); else await api.diningTableCreate(payload);
          toast(isEdit ? '桌台已更新' : '桌台已创建', 'success');
          close();
          await renderTables($('#dining-content'));
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      });
    }
  });
}

async function deleteTable(table) {
  if (!table) return;
  const ok = await confirmDialog({ title: '删除桌台', message: `确定删除桌台 <strong>${escapeHtml(table.name)}</strong> 吗？正在使用或存在历史会话的桌台会被后端拒绝删除。`, danger: true, confirmText: '删除' });
  if (!ok) return;
  try {
    await api.diningTableDelete(table.id);
    toast('桌台已删除', 'success');
    await renderTables($('#dining-content'));
  } catch (error) { toastError(error); }
}

function showQr(table) {
  if (!table) return;
  const content = table.qrContent || table.qrToken || '';
  openModal({
    title: '桌台二维码',
    subtitle: `${table.name || ''} · 用户扫码后可开台或加入同桌会话`,
    size: 'modal-sm',
    body: `<div class="qr-panel" style="flex-direction:column;align-items:stretch">
      <div class="qr-canvas" id="qr-canvas" style="align-self:center"></div>
      <div>
        <div class="text-sm muted" style="margin-bottom:4px">二维码内容</div>
        <div class="qr-content" id="qr-content-text">${escapeHtml(content || '令牌为空')}</div>
      </div>
    </div>`,
    footer: `<button class="btn btn-ghost" type="button" data-copy>复制内容</button>
             <button class="btn btn-primary" type="button" data-refresh>刷新二维码</button>`,
    onMount: (modal, close) => {
      const box = modal.querySelector('#qr-canvas');
      const textEl = modal.querySelector('#qr-content-text');
      const draw = (value) => {
        box.innerHTML = '';
        if (!value) { box.innerHTML = '<span class="text-sm muted">无内容</span>'; return; }
        if (window.QRCode) {
          try {
            new window.QRCode(box, { text: value, width: 160, height: 160, colorDark: '#18222d', colorLight: '#ffffff', correctLevel: window.QRCode.CorrectLevel.M });
          } catch (e) {
            box.innerHTML = '<span class="text-sm muted">二维码生成失败</span>';
          }
        } else {
          box.innerHTML = '<span class="text-sm muted">二维码库未加载</span>';
        }
      };
      draw(content);
      modal.querySelector('[data-copy]').addEventListener('click', async () => {
        try {
          await navigator.clipboard.writeText(content);
          toast('二维码内容已复制', 'success');
        } catch {
          toast('复制失败，请手动选择内容复制', 'warn');
        }
      });
      modal.querySelector('[data-refresh]').addEventListener('click', async (event) => {
        const btn = event.currentTarget;
        setBtnLoading(btn, true, '刷新中…');
        try {
          const vo = await api.diningTableRefreshQr(table.id);
          const newContent = (vo && (vo.qrContent || vo.qrToken)) || '';
          textEl.textContent = newContent;
          draw(newContent);
          toast('二维码已刷新，旧码立即失效', 'success');
          await loadTables();
        } catch (error) { toastError(error); } finally { setBtnLoading(btn, false); }
      });
    }
  });
}

async function openOpenTable(tableId) {
  if (!state.tables.length) await loadTables();
  const freeTables = state.tables.filter((t) => t.status === 0);
  let reservations = [];
  try { reservations = await api.diningReservations({ status: 2 }) || []; } catch { reservations = []; }
  const table = state.tables.find((t) => Number(t.id) === Number(tableId));
  openModal({
    title: '开台',
    subtitle: table ? `${table.name} · ${table.seats} 座` : '员工代客开台',
    size: 'modal-sm',
    body: `<form id="open-form" class="form-grid" style="grid-template-columns:1fr" autocomplete="off">
      <div class="form-item"><label>桌台<em>*</em></label><select class="select" name="tableId">
        ${freeTables.map((t) => `<option value="${t.id}" ${Number(t.id) === Number(tableId) ? 'selected' : ''}>${escapeHtml(t.name)} · ${number(t.seats)} 座 ${escapeHtml(t.area || '')}</option>`).join('')}
      </select>${freeTables.length ? '' : '<span class="form-hint" style="color:var(--danger)">当前没有空闲桌台</span>'}</div>
      <div class="form-item"><label>就餐人数<em>*</em></label><input class="input" name="guestCount" type="number" min="1" step="1" value="2"></div>
      <div class="form-item"><label>顾客昵称</label><input class="input" name="nickname" maxlength="20" placeholder="默认使用员工账号名"></div>
      <div class="form-item"><label>关联预约</label><select class="select" name="reservationId"><option value="">不关联</option>
        ${reservations.map((r) => `<option value="${r.id}">#${r.id} ${escapeHtml(r.contactName || '')} · ${escapeHtml(fmtDate(r.reserveTime))}</option>`).join('')}
      </select></div>
      <div class="form-item"><label>备注</label><textarea class="textarea" name="remark" maxlength="100" placeholder="选填"></textarea></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>确认开台</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#open-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const tableIdValue = Number(form.tableId.value);
        const guestCount = Number(form.guestCount.value);
        if (!tableIdValue) { errorEl.hidden = false; errorEl.textContent = '请选择空闲桌台'; return; }
        if (!Number.isFinite(guestCount) || guestCount < 1) { errorEl.hidden = false; errorEl.textContent = '就餐人数至少为 1'; return; }
        const payload = {
          tableId: tableIdValue, guestCount,
          nickname: form.nickname.value.trim() || undefined,
          remark: form.remark.value.trim() || undefined
        };
        if (form.reservationId.value) payload.reservationId = Number(form.reservationId.value);
        setBtnLoading(saveBtn, true, '开台中…');
        try {
          await api.diningOpen(payload);
          toast('开台成功', 'success');
          close();
          await refreshAll();
          switchTab('sessions');
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      });
    }
  });
}

async function renderReservations(container) {
  let list = [];
  try {
    list = await api.diningReservations({
      status: state.filters.reserveStatus === '' ? undefined : Number(state.filters.reserveStatus),
      date: state.filters.reserveDate || undefined
    }) || [];
  } catch (error) { toastError(error); }
  state.reservations = list;
  const rows = list.length ? list.map((r) => {
    const info = RESERVE_STATUS[r.status] || { text: `状态 ${r.status}`, tone: 'default' };
    const actions = [];
    if (r.status === 1) actions.push(`<button class="btn btn-primary btn-xs" data-rc="${r.id}">确认</button>`);
    if (r.status === 1 || r.status === 2) {
      actions.push(`<button class="btn btn-soft btn-xs" data-ra="${r.id}">到店</button>`);
      actions.push(`<button class="btn btn-danger-ghost btn-xs" data-rx="${r.id}">取消</button>`);
    }
    return `<tr>
      <td class="mono">#${r.id}</td>
      <td>${r.tableId ? `桌台 ${r.tableId}` : '—'}</td>
      <td><div class="cell-strong">${escapeHtml(r.contactName || '—')}</div><div class="text-sm muted">${escapeHtml(r.contactPhone || '')}</div></td>
      <td>${escapeHtml(fmtDate(r.reserveTime))}</td>
      <td class="num">${number(r.guestCount)} 人</td>
      <td>${statusTag(info.text, info.tone)}</td>
      <td class="text-sm muted">${escapeHtml(r.remark || '—')}</td>
      <td><div class="row-actions">${actions.join('') || '<span class="text-sm muted">无操作</span>'}</div></td>
    </tr>`;
  }).join('') : emptyTable(8, '没有预约记录', '用户端提交预约后会出现在这里');

  container.innerHTML = `
    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <select id="r-status" class="select filter-select">
            <option value="">全部状态</option>
            ${Object.entries(RESERVE_STATUS).map(([k, v]) => `<option value="${k}" ${state.filters.reserveStatus === k ? 'selected' : ''}>${v.text}</option>`).join('')}
          </select>
          <input id="r-date" class="input filter-date" type="date" value="${escapeHtml(state.filters.reserveDate)}">
          <button id="r-search" class="btn btn-primary" type="button">查询</button>
          <button id="r-reset" class="btn btn-ghost" type="button">重置</button>
        </div>
      </div>
      <div class="table-wrap"><table class="data-table">
        <thead><tr><th style="width:70px">预约号</th><th style="width:90px">桌台</th><th style="min-width:140px">联系人</th><th style="width:150px">预约时间</th><th style="width:80px">人数</th><th style="width:90px">状态</th><th style="min-width:140px">备注</th><th style="width:190px">操作</th></tr></thead>
        <tbody>${rows}</tbody></table></div>
    </div>`;

  $('#r-search').onclick = () => {
    state.filters.reserveStatus = $('#r-status').value;
    state.filters.reserveDate = $('#r-date').value;
    renderReservations(container);
  };
  $('#r-reset').onclick = () => {
    state.filters.reserveStatus = ''; state.filters.reserveDate = '';
    renderReservations(container);
  };
  container.querySelector('.table-wrap').addEventListener('click', async (event) => {
    const confirmBtn = event.target.closest('[data-rc]');
    const arriveBtn = event.target.closest('[data-ra]');
    const cancelBtn = event.target.closest('[data-rx]');
    try {
      if (confirmBtn) {
        await api.diningReservationConfirm(Number(confirmBtn.dataset.rc));
        toast('预约已确认', 'success');
        await renderReservations(container);
      } else if (arriveBtn) {
        await api.diningReservationArrive(Number(arriveBtn.dataset.ra));
        toast('已标记到店，可在桌台页开台', 'success');
        await renderReservations(container);
        await loadTables();
      } else if (cancelBtn) {
        const ok = await confirmDialog({ title: '取消预约', message: '确定取消该预约吗？', danger: true, confirmText: '取消预约' });
        if (!ok) return;
        await api.diningReservationCancel(Number(cancelBtn.dataset.rx));
        toast('预约已取消', 'success');
        await renderReservations(container);
      }
    } catch (error) { toastError(error); }
  });
}

async function renderSessions(container) {
  if (!state.tables.length) await loadTables();
  let list = [];
  try {
    list = await api.diningSessions({
      status: state.filters.sessionStatus === '' ? undefined : Number(state.filters.sessionStatus),
      tableId: state.filters.sessionTable || undefined
    }) || [];
  } catch (error) { toastError(error); }
  state.sessions = list;
  const rows = list.length ? list.map((s) => {
    const info = SESSION_STATUS[s.status] || { text: `状态 ${s.status}`, tone: 'default' };
    const actions = [`<button class="btn btn-ghost btn-xs" data-detail="${s.id}">详情</button>`];
    if (s.status === 1) {
      actions.push(`<button class="btn btn-ghost btn-xs" data-transfer="${s.id}">转台</button>`);
      actions.push(`<button class="btn btn-ghost btn-xs" data-merge="${s.id}">并桌</button>`);
    }
    if (s.status === 1 || s.status === 2) {
      actions.push(`<button class="btn btn-primary btn-xs" data-checkout="${s.id}">结账</button>`);
      actions.push(`<button class="btn btn-danger-ghost btn-xs" data-cancel="${s.id}">关闭会话</button>`);
    }
    return `<tr>
      <td><div class="cell-strong mono">${escapeHtml(s.sessionNo || `#${s.id}`)}</div><div class="text-sm muted">${escapeHtml(fmtDate(s.openTime))}</div></td>
      <td><div class="cell-strong">${escapeHtml(s.tableName || '—')}</div><div class="text-sm muted">${number(s.memberCount)} 位成员</div></td>
      <td class="num">${number(s.guestCount)} 人</td>
      <td class="num cell-strong">${money(s.totalAmount)}<div class="text-sm muted">已付 ${money(s.paidAmount)} · 未付 ${money(s.unpaidAmount)}</div></td>
      <td>${statusTag(info.text, info.tone)}${s.aaEnabled === 1 ? '<span class="tag tag-info" style="margin-left:4px">AA</span>' : ''}</td>
      <td><div class="row-actions">${actions.join('')}</div></td>
    </tr>`;
  }).join('') : emptyTable(6, '没有就餐会话', '开台后会话会出现在这里');

  const tableOptions = `<option value="">全部桌台</option>` + state.tables.map((t) => `<option value="${t.id}" ${String(state.filters.sessionTable) === String(t.id) ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('');
  container.innerHTML = `
    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <select id="s-status" class="select filter-select">
            <option value="">全部状态</option>
            ${Object.entries(SESSION_STATUS).map(([k, v]) => `<option value="${k}" ${state.filters.sessionStatus === k ? 'selected' : ''}>${v.text}</option>`).join('')}
          </select>
          <select id="s-table" class="select filter-select">${tableOptions}</select>
          <button id="s-search" class="btn btn-primary" type="button">查询</button>
          <button id="s-reset" class="btn btn-ghost" type="button">重置</button>
        </div>
        <div class="toolbar-right"><button id="s-open" class="btn btn-primary" type="button">代客开台</button></div>
      </div>
      <div class="table-wrap"><table class="data-table">
        <thead><tr><th style="min-width:170px">会话号 / 开台时间</th><th style="min-width:110px">桌台</th><th style="width:70px">人数</th><th style="min-width:180px">金额</th><th style="width:100px">状态</th><th style="min-width:220px">操作</th></tr></thead>
        <tbody>${rows}</tbody></table></div>
    </div>`;

  $('#s-search').onclick = () => {
    state.filters.sessionStatus = $('#s-status').value;
    state.filters.sessionTable = $('#s-table').value;
    renderSessions(container);
  };
  $('#s-reset').onclick = () => {
    state.filters.sessionStatus = ''; state.filters.sessionTable = '';
    renderSessions(container);
  };
  $('#s-open').onclick = () => {
    const free = state.tables.find((t) => t.status === 0);
    if (!free) { toast('当前没有空闲桌台', 'warn'); return; }
    openOpenTable(free.id);
  };
  container.querySelector('.table-wrap').addEventListener('click', (event) => {
    const detail = event.target.closest('[data-detail]');
    const transfer = event.target.closest('[data-transfer]');
    const merge = event.target.closest('[data-merge]');
    const checkout = event.target.closest('[data-checkout]');
    const cancel = event.target.closest('[data-cancel]');
    const id = detail?.dataset.detail || transfer?.dataset.transfer || merge?.dataset.merge || checkout?.dataset.checkout || cancel?.dataset.cancel;
    if (detail) { showSession(Number(id)); return; }
    if (transfer) { openTransfer(Number(id)); return; }
    if (merge) { openMerge(Number(id)); return; }
    if (checkout) { openCheckout(Number(id)); return; }
    if (cancel) { closeDiningSession(Number(id)); }
  });
}

async function showSession(id) {
  const drawer = openDrawer({
    title: '会话详情',
    subtitle: '加载中…',
    body: '<div class="empty-block">正在读取会话…</div>',
    onMount: (el, close) => {
      el.addEventListener('click', (event) => {
        const btn = event.target.closest('[data-drawer-close]');
        if (!btn) return;
        close();
        closeDiningSession(Number(btn.dataset.drawerClose));
      });
    }
  });
  try {
    const s = await api.diningSession(id);
    if (!s) throw new Error('会话不存在');
    drawer.el.querySelector('.drawer-header p').textContent = `${s.sessionNo || `#${s.id}`} · ${s.tableName || ''}`;
    const info = SESSION_STATUS[s.status] || { text: `状态 ${s.status}`, tone: 'default' };
    const members = s.members || [];
    const cart = s.cartItems || [];
    const orders = s.orders || [];
    drawer.body.innerHTML = `
      <div class="session-summary" style="margin-bottom:14px">
        <div class="mini-stat"><span class="mini-stat-label">总金额</span><span class="mini-stat-value">${money(s.totalAmount)}</span></div>
        <div class="mini-stat"><span class="mini-stat-label">已支付</span><span class="mini-stat-value" style="color:var(--success)">${money(s.paidAmount)}</span></div>
        <div class="mini-stat"><span class="mini-stat-label">待支付</span><span class="mini-stat-value" style="color:var(--danger)">${money(s.unpaidAmount)}</span></div>
      </div>
      <div class="detail-list" style="margin-bottom:16px">
        <div class="detail-row"><span>状态</span><strong>${statusTag(info.text, info.tone)}</strong></div>
        <div class="detail-row"><span>桌台</span><strong>${escapeHtml(s.tableName || '—')}${s.tableNames && s.tableNames.length > 1 ? `（并桌：${escapeHtml(s.tableNames.join(' / '))}）` : ''}</strong></div>
        <div class="detail-row"><span>就餐人数</span><strong>${number(s.guestCount)} 人 · ${number(s.memberCount)} 位成员</strong></div>
        <div class="detail-row"><span>AA 结账</span><strong>${s.aaEnabled === 1 ? '已开启' : '未开启'}</strong></div>
        <div class="detail-row"><span>人均金额</span><strong>${money(s.perPersonAmount)}</strong></div>
        <div class="detail-row"><span>开台时间</span><strong>${escapeHtml(fmtDate(s.openTime))}</strong></div>
        <div class="detail-row"><span>结账时间</span><strong>${escapeHtml(fmtDate(s.checkoutTime))}</strong></div>
        <div class="detail-row"><span>备注</span><strong>${escapeHtml(s.remark || '—')}</strong></div>
      </div>
      <div class="card" style="box-shadow:none;margin-bottom:14px">
        <div class="card-head" style="padding:12px 14px 8px"><h3 style="font-size:14px">就餐成员</h3><span class="tag">${members.length} 人</span></div>
        <div class="card-body" style="padding:0 14px 12px">
          ${members.length ? `<div class="detail-list">${members.map((m) => `<div class="detail-row"><span>${escapeHtml(m.nickname || `用户 ${m.userId}`)} ${m.isOwner === 1 ? '<span class="tag tag-brand" style="margin-left:4px">台主</span>' : ''}</span><strong>${money(m.paidAmount)} · ${m.payStatus === 1 ? '已支付' : '未支付'}</strong></div>`).join('')}</div>` : '<div class="text-sm muted">暂无成员</div>'}
        </div>
      </div>
      <div class="card" style="box-shadow:none;margin-bottom:14px">
        <div class="card-head" style="padding:12px 14px 8px"><h3 style="font-size:14px">共享购物车</h3><span class="tag">${cart.length} 项</span></div>
        <div class="card-body" style="padding:0 14px 12px">
          ${cart.length ? `<div class="detail-list">${cart.map((it) => `<div class="detail-row"><span>${escapeHtml(it.name || '')} × ${number(it.number)}</span><strong>${money(it.amount)}</strong></div>`).join('')}</div>` : '<div class="text-sm muted">购物车为空</div>'}
        </div>
      </div>
      <div class="card" style="box-shadow:none">
        <div class="card-head" style="padding:12px 14px 8px"><h3 style="font-size:14px">关联订单</h3><span class="tag">${orders.length} 单</span></div>
        <div class="card-body" style="padding:0 14px 12px">
          ${orders.length ? `<div class="detail-list">${orders.map((o) => `<div class="detail-row"><span class="mono">${escapeHtml(o.number || `#${o.id}`)}</span><strong>${money(o.amount)} · ${(SESSION_STATUS[1] && '')}${o.status === 5 ? '已完成' : o.status === 6 ? '已取消' : '进行中'}</strong></div>`).join('')}</div>` : '<div class="text-sm muted">暂无订单</div>'}
        </div>
      </div>
      ${s.status === 1 || s.status === 2 ? `<button class="btn btn-danger-ghost" style="width:100%;margin-top:16px" data-drawer-close="${s.id}">关闭会话</button>` : ''}`;
  } catch (error) {
    drawer.body.innerHTML = `<div class="empty-block"><strong>读取失败</strong><span>${escapeHtml(error.message)}</span></div>`;
  }
}

function openTransfer(id) {
  const free = state.tables.filter((t) => t.status === 0);
  openModal({
    title: '转台',
    subtitle: '把当前会话迁移到空闲桌台，原桌台会释放',
    size: 'modal-sm',
    body: `<form id="transfer-form" class="form-grid" style="grid-template-columns:1fr">
      <div class="form-item"><label>目标桌台<em>*</em></label><select class="select" name="targetTableId">
        ${free.map((t) => `<option value="${t.id}">${escapeHtml(t.name)} · ${number(t.seats)} 座 ${escapeHtml(t.area || '')}</option>`).join('')}
      </select>${free.length ? '' : '<span class="form-hint" style="color:var(--danger)">没有空闲桌台可转</span>'}</div>
      <div class="form-item"><label>备注</label><textarea class="textarea" name="remark" placeholder="选填"></textarea></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>确认转台</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#transfer-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.onclick = async () => {
        errorEl.hidden = true;
        const targetTableId = Number(form.targetTableId.value);
        if (!targetTableId) { errorEl.hidden = false; errorEl.textContent = '请选择目标桌台'; return; }
        setBtnLoading(saveBtn, true, '转台中…');
        try {
          await api.diningTransfer(id, { targetTableId, remark: form.remark.value.trim() || undefined });
          toast('转台成功', 'success');
          close();
          await refreshAll();
          await renderSessions($('#dining-content'));
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      };
    }
  });
}

async function openMerge(id) {
  let sessions = [];
  try { sessions = await api.diningSessions({ status: 1 }) || []; } catch { sessions = []; }
  const others = sessions.filter((s) => Number(s.id) !== Number(id));
  openModal({
    title: '并桌',
    subtitle: '把当前会话并入目标会话，源会话会被取消并释放桌台',
    size: 'modal-sm',
    body: `<form id="merge-form" class="form-grid" style="grid-template-columns:1fr">
      <div class="form-item"><label>目标会话<em>*</em></label><select class="select" name="targetId">
        ${others.map((s) => `<option value="${s.id}">${escapeHtml(s.sessionNo || `#${s.id}`)} · ${escapeHtml(s.tableName || '')} · ${money(s.totalAmount)}</option>`).join('')}
      </select>${others.length ? '' : '<span class="form-hint" style="color:var(--danger)">没有其他就餐中的会话</span>'}</div>
      <div class="form-item"><label>备注</label><textarea class="textarea" name="remark" placeholder="选填"></textarea></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>确认并桌</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#merge-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.onclick = async () => {
        errorEl.hidden = true;
        const targetId = Number(form.targetId.value);
        if (!targetId) { errorEl.hidden = false; errorEl.textContent = '请选择目标会话'; return; }
        setBtnLoading(saveBtn, true, '并桌中…');
        try {
          await api.diningMerge(targetId, { sourceSessionIds: [id], remark: form.remark.value.trim() || undefined });
          toast('并桌成功', 'success');
          close();
          await refreshAll();
          await renderSessions($('#dining-content'));
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      };
    }
  });
}

async function openCheckout(id) {
  let session = null;
  try { session = await api.diningSession(id); } catch { session = null; }
  openModal({
    title: '整单结账',
    subtitle: session ? `${session.sessionNo || ''} · ${session.tableName || ''} · 待付 ${money(session.unpaidAmount)}` : '会话结账',
    size: 'modal-sm',
    body: `<form id="checkout-form" class="form-grid" style="grid-template-columns:1fr">
      <div class="form-item"><label>支付方式</label><select class="select" name="payMethod">
        <option value="1">微信</option><option value="2">支付宝</option><option value="3" selected>现金</option>
      </select></div>
      <div class="form-item"><label style="display:flex;align-items:center;gap:8px"><input type="checkbox" name="settleUnpaid" checked> 强制结清未支付的 AA 账单</label><span class="form-hint">勾选后会把未支付成员标记为已支付</span></div>
      <div class="form-item"><label>备注</label><textarea class="textarea" name="remark" placeholder="选填，例如：前台现金结账"></textarea></div>
      <div class="mini-stat"><span class="mini-stat-label">应收金额</span><span class="mini-stat-value">${money(session ? session.totalAmount : 0)}</span></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>确认结账</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#checkout-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.onclick = async () => {
        errorEl.hidden = true;
        setBtnLoading(saveBtn, true, '结账中…');
        try {
          await api.diningCheckout({
            sessionId: id,
            payMethod: Number(form.payMethod.value),
            settleUnpaid: form.settleUnpaid.checked,
            remark: form.remark.value.trim() || undefined
          });
          toast('结账成功，桌台已空闲，顾客会话已结束', 'success');
          close();
          await refreshAll();
          await renderTabContent($('#dining-content'));
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      };
    }
  });
}

async function closeDiningSession(id) {
  const reason = await promptDialog({
    title: '关闭会话',
    message: '关闭后将结束本桌会话，未完成订单会被取消、共享购物车清空并释放桌台；顾客端会退出会话。已支付/已完成订单保留。',
    label: '关闭原因',
    value: '管理员关闭会话',
    placeholder: '请填写关闭原因',
    required: true,
    multiline: true,
    danger: true,
    confirmText: '确认关闭'
  });
  if (!reason) return;
  try {
    await api.diningClose(id, reason);
    toast('会话已关闭，桌台已空闲，顾客已退出', 'success');
    await refreshAll();
    await renderTabContent($('#dining-content'));
  } catch (error) { toastError(error); }
}

async function renderReport(container) {
  const beginInput = container.dataset.begin || '';
  const endInput = container.dataset.end || '';
  container.innerHTML = `
    <div class="card">
      <div class="toolbar">
        <div class="toolbar-left">
          <input id="dr-begin" class="input filter-date" type="datetime-local" value="${escapeHtml(beginInput)}" title="开始时间">
          <span class="muted text-sm">至</span>
          <input id="dr-end" class="input filter-date" type="datetime-local" value="${escapeHtml(endInput)}" title="结束时间">
          <button id="dr-search" class="btn btn-primary" type="button">生成报表</button>
          <button id="dr-today" class="btn btn-ghost" type="button">今日</button>
        </div>
        <div class="toolbar-right"><span class="text-sm muted">统计口径：已结账会话</span></div>
      </div>
      <div class="card-body card-body-pad" id="dr-body"><div class="empty-block">加载中…</div></div>
    </div>`;

  const setRangeToday = () => {
    const now = new Date();
    $('#dr-begin').value = toDateTimeInput(new Date(now.getFullYear(), now.getMonth(), now.getDate(), 0, 0));
    $('#dr-end').value = toDateTimeInput(now);
  };
  if (!$('#dr-begin').value) setRangeToday();
  $('#dr-today').onclick = () => { setRangeToday(); loadReport(container); };
  $('#dr-search').onclick = () => { container.dataset.begin = $('#dr-begin').value; container.dataset.end = $('#dr-end').value; loadReport(container); };
  await loadReport(container);
}

async function loadReport(container) {
  const body = $('#dr-body');
  const begin = fromDateTimeInput($('#dr-begin').value);
  const end = fromDateTimeInput($('#dr-end').value);
  if (!begin || !end) { toast('请选择开始和结束时间', 'warn'); return; }
  body.innerHTML = '<div class="empty-block">正在计算…</div>';
  try {
    const report = await api.diningReport({ begin, end });
    if (!report) throw new Error('报表返回为空');
    const daily = report.daily || [];
    body.innerHTML = `
      <div class="stat-grid" style="margin-bottom:16px">
        <div class="stat-card"><div class="stat-top"><span class="stat-ic">台</span>已结账会话</div><div class="stat-value">${number(report.sessionCount)}</div><div class="stat-sub">桌台总数 ${number(report.tableCount)}</div></div>
        <div class="stat-card"><div class="stat-top"><span class="stat-ic green">¥</span>总营业额</div><div class="stat-value">${money(report.totalRevenue)}</div><div class="stat-sub">已结账会话合计</div></div>
        <div class="stat-card"><div class="stat-top"><span class="stat-ic blue">人</span>就餐人数</div><div class="stat-value">${number(report.totalGuests)}</div><div class="stat-sub">全部已结账会话</div></div>
        <div class="stat-card"><div class="stat-top"><span class="stat-ic amber">均</span>客单价</div><div class="stat-value">${money(report.avgAmount)}</div><div class="stat-sub">营业额 / 会话数</div></div>
        <div class="stat-card"><div class="stat-top"><span class="stat-ic slate">均</span>平均人数</div><div class="stat-value">${number(report.avgGuests, 1)}</div><div class="stat-sub">人数 / 会话数</div></div>
        <div class="stat-card"><div class="stat-top"><span class="stat-ic">转</span>桌台周转率</div><div class="stat-value">${number(report.turnoverRate, 2)}</div><div class="stat-sub">结账会话 / 启用桌台</div></div>
      </div>
      <div class="grid-2-1">
        <div class="card" style="box-shadow:none">
          <div class="card-head" style="padding:14px 16px 8px"><div><h3 style="font-size:14px">每日营业额</h3><p>按结账时间聚合</p></div></div>
          <div class="card-body"><div class="chart-box" style="height:260px">${daily.length ? '<canvas id="dr-chart"></canvas>' : '<div class="empty-block" style="height:100%"><strong>暂无数据</strong><span class="text-sm">当前时间范围没有已结账会话</span></div>'}</div></div>
        </div>
        <div class="card" style="box-shadow:none">
          <div class="card-head" style="padding:14px 16px 8px"><div><h3 style="font-size:14px">每日明细</h3><p>${daily.length} 天有结账数据</p></div></div>
          <div class="card-body" style="padding:0 16px 14px">
            <div class="detail-list" style="max-height:260px;overflow:auto">
              ${daily.length ? daily.map((d) => `<div class="detail-row"><span>${escapeHtml(String(d.day || ''))}</span><strong>${number(d.session_count)} 单 · ${money(d.revenue)} · ${number(d.guests)} 人</strong></div>`).join('') : '<div class="text-sm muted">暂无每日数据</div>'}
            </div>
          </div>
        </div>
      </div>`;
    if (reportChart) { reportChart.destroy(); reportChart = null; }
    if (daily.length) {
      const canvas = $('#dr-chart');
      reportChart = barChart(canvas, {
        labels: daily.map((d) => String(d.day || '').slice(5)),
        series: [{ name: '营业额', data: daily.map((d) => Number(d.revenue) || 0), color: '#e8552d' }],
        yFormat: (v) => money(v)
      });
    }
  } catch (error) {
    toastError(error);
    body.innerHTML = `<div class="empty-block"><strong>报表读取失败</strong><span>${escapeHtml(error.message)}</span></div>`;
  }
}

function switchTab(tab) {
  state.tab = tab;
  document.querySelectorAll('#dining-tabs .tab').forEach((el) => el.classList.toggle('active', el.dataset.tab === tab));
  renderTabContent($('#dining-content'));
}

async function renderTabContent(container) {
  if (!container) return;
  if (reportChart) { reportChart.destroy(); reportChart = null; }
  if (state.tab === 'tables') await renderTables(container);
  else if (state.tab === 'reservations') await renderReservations(container);
  else if (state.tab === 'sessions') await renderSessions(container);
  else await renderReport(container);
}

async function refreshAll() {
  await loadTables();
}

export async function render(root) {
  state.tab = 'tables';
  root.innerHTML = `<div class="section-stack"><div class="card"><div class="card-body card-body-pad">${tabsBar()}</div></div><div id="dining-content"></div></div>`;
  root.querySelector('#dining-tabs').addEventListener('click', (event) => {
    const btn = event.target.closest('[data-tab]');
    if (btn) switchTab(btn.dataset.tab);
  });
  await renderTabContent($('#dining-content'));
}

export function destroy() {
  if (reportChart) { reportChart.destroy(); reportChart = null; }
}


