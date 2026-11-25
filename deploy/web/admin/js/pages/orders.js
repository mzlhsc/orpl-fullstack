/* 订单管理 */
import { api } from '../api.js';
import { $, escapeHtml, fmtDate, fromDateTimeInput, money, number, statusTone } from '../util.js';
import { bindPager, confirmDialog, emptyTable, openDrawer, pagerHtml, promptDialog, statusTag, toast, toastError } from '../ui.js';

const ORDER_STATUS = {
  1: { text: '待付款', tone: 'warn' },
  2: { text: '待接单', tone: 'brand' },
  3: { text: '已接单', tone: 'info' },
  5: { text: '已完成', tone: 'success' },
  6: { text: '已取消', tone: 'danger' },
  7: { text: '退款', tone: 'danger' }
};
const PAY_STATUS = { 0: '未支付', 1: '已支付', 2: '已退款' };

const state = {
  page: 1,
  pageSize: 10,
  number: '',
  phone: '',
  status: '',
  beginTime: '',
  endTime: '',
  total: 0,
  stats: null,
  autoAccept: { takeout: false, dining: false },
  autoAcceptLoaded: false,
  autoAcceptSaving: false,
  loading: false
};

function statusInfo(status) {
  return ORDER_STATUS[status] || { text: `状态 ${status}`, tone: 'default' };
}

function statBlock(key, label, value, tone) {
  return `<button class="stat-card" type="button" data-stat="${key}" style="text-align:left;border:1px solid var(--line);cursor:pointer;padding:14px 16px">
    <div class="stat-top"><span class="stat-ic ${tone}">
      <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3.5h12v17l-2-1.4-2 1.4-2-1.4-2 1.4-2-1.4-2 1.4v-17Z"/><path d="M9 8h6M9 12h6"/></svg>
    </span><span>${escapeHtml(label)}</span></div>
    <div class="stat-value" style="font-size:24px">${number(value)}</div>
    <div class="stat-sub">点击筛选该状态</div>
  </button>`;
}

function autoAcceptCard() {
  return `<div class="card" id="auto-accept-card">
    <div class="card-head">
      <div><h2>接单设置</h2><p>开启后，对应类型的新订单会直接进入「已接单」</p></div>
      <span class="tag" id="auto-accept-tag">读取中…</span>
    </div>
    <div class="card-body card-body-pad">
      <div class="auto-accept-grid">
        <div class="auto-accept-item">
          <div class="auto-accept-copy">
            <strong>外卖自动接单</strong>
            <span>新外卖订单提交后自动接单，无需人工点击</span>
          </div>
          <button class="switch brand" id="auto-takeout" data-auto="takeout" type="button" role="switch" aria-checked="false" aria-label="外卖自动接单" disabled></button>
        </div>
        <div class="auto-accept-item">
          <div class="auto-accept-copy">
            <strong>堂食自动接单</strong>
            <span>同桌加菜提交后自动接单，直接进入备餐</span>
          </div>
          <button class="switch brand" id="auto-dining" data-auto="dining" type="button" role="switch" aria-checked="false" aria-label="堂食自动接单" disabled></button>
        </div>
      </div>
      <div class="auto-accept-foot">开关只影响设置之后的新订单，历史订单状态不会追溯变更。</div>
    </div>
  </div>`;
}

function paintAutoAccept() {
  const takeout = $('#auto-takeout');
  const dining = $('#auto-dining');
  const tag = $('#auto-accept-tag');
  if (!takeout || !dining) return;
  const enabledCount = Number(state.autoAccept.takeout) + Number(state.autoAccept.dining);
  [takeout, dining].forEach((sw) => {
    sw.disabled = !state.autoAcceptLoaded || state.autoAcceptSaving;
  });
  takeout.classList.toggle('on', state.autoAccept.takeout);
  takeout.setAttribute('aria-checked', state.autoAccept.takeout ? 'true' : 'false');
  dining.classList.toggle('on', state.autoAccept.dining);
  dining.setAttribute('aria-checked', state.autoAccept.dining ? 'true' : 'false');
  if (!tag) return;
  tag.textContent = !state.autoAcceptLoaded
    ? '读取失败'
    : enabledCount === 2 ? '两项已开启' : (enabledCount === 1 ? '1 项已开启' : '均未开启');
  tag.className = enabledCount === 2 ? 'tag tag-success' : (enabledCount === 1 ? 'tag tag-info' : 'tag');
}

async function loadAutoAccept() {
  try {
    const data = await api.shopAutoAccept();
    state.autoAccept = {
      takeout: !!(data && data.takeoutAutoAccept),
      dining: !!(data && data.diningAutoAccept)
    };
    state.autoAcceptLoaded = true;
  } catch (error) {
    state.autoAcceptLoaded = false;
    console.warn(error);
  }
  paintAutoAccept();
}

async function toggleAutoAccept(type) {
  if (!state.autoAcceptLoaded || state.autoAcceptSaving) return;
  const key = type === 'dining' ? 'dining' : 'takeout';
  const next = !state.autoAccept[key];
  state.autoAcceptSaving = true;
  paintAutoAccept();
  try {
    const body = type === 'dining'
      ? { diningAutoAccept: next }
      : { takeoutAutoAccept: next };
    const data = await api.shopAutoAcceptSet(body);
    if (data) {
      state.autoAccept = {
        takeout: !!data.takeoutAutoAccept,
        dining: !!data.diningAutoAccept
      };
    } else {
      state.autoAccept[key] = next;
    }
    toast(`${type === 'dining' ? '堂食' : '外卖'}自动接单已${next ? '开启' : '关闭'}`, 'success');
  } catch (error) {
    toastError(error);
  } finally {
    state.autoAcceptSaving = false;
    paintAutoAccept();
  }
}

function paintStats() {
  const box = $('#order-stats');
  if (!box) return;
  if (!state.stats) {
    box.innerHTML = '<div class="text-sm muted">订单状态统计读取中…</div>';
    return;
  }
  const s = state.stats;
  box.innerHTML = `
    <div class="stat-grid" style="grid-template-columns:repeat(2,minmax(0,1fr))">
      ${statBlock('2', '待接单', s.toBeConfirmed, '')}
      ${statBlock('3', '已接单 / 待处理', s.confirmed, 'blue')}
    </div>`;
}

async function loadStats() {
  try {
    state.stats = await api.orderStatistics();
  } catch (error) {
    state.stats = null;
    console.warn(error);
  }
  paintStats();
}

function renderTable(pageData) {
  const holder = $('#order-table');
  if (!holder) return;
  const list = (pageData && pageData.list) || [];
  const rows = list.length
    ? list.map((o) => {
      const info = statusInfo(o.status);
      const payStatus = Number(o.payStatus || 0);
      const actions = [];
      if (o.status === 2) {
        actions.push(`<button class="btn btn-primary btn-xs" data-action="confirm" data-id="${o.id}">接单</button>`);
        actions.push(`<button class="btn btn-danger-ghost btn-xs" data-action="reject" data-id="${o.id}">拒单</button>`);
      }
      if (o.status === 3 && o.orderType !== 2) actions.push(`<button class="btn btn-primary btn-xs" data-action="complete" data-id="${o.id}">完成</button>`);
      if ([2, 3].includes(o.status)) actions.push(`<button class="btn btn-ghost btn-xs" data-action="cancel" data-id="${o.id}">取消</button>`);
      if (o.status !== 6 && payStatus === 0) {
        actions.push(`<button class="btn btn-soft btn-xs" data-action="mark-paid" data-id="${o.id}">${o.status === 5 ? '补记已支付' : '标记已支付'}</button>`);
      }
      if (o.status !== 5 && o.status !== 6 && payStatus === 1) {
        actions.push(`<button class="btn btn-ghost btn-xs" data-action="mark-unpaid" data-id="${o.id}">标记未支付</button>`);
      }
      actions.push(`<button class="btn btn-ghost btn-xs" data-action="detail" data-id="${o.id}">详情</button>`);
      return `<tr>
        <td><div class="cell-strong mono" style="font-size:12px">${escapeHtml(o.number || `#${o.id}`)}</div><div class="text-sm muted">${escapeHtml(fmtDate(o.orderTime))}</div></td>
        <td><div class="cell-strong">${escapeHtml(o.consignee || o.userName || '—')}</div><div class="text-sm muted">${escapeHtml(o.phone || '')}</div></td>
        <td>${o.orderType === 2 ? '<span class="tag tag-info">堂食</span>' : '<span class="tag">外卖</span>'}</td>
        <td class="num cell-strong">${money(o.amount)}</td>
        <td>${statusTag(PAY_STATUS[payStatus] || '未支付', payStatus === 1 ? 'success' : payStatus === 2 ? 'danger' : 'default')}</td>
        <td>${statusTag(info.text, info.tone)}</td>
        <td><div class="row-actions">${actions.join('')}</div></td>
      </tr>`;
    }).join('')
    : emptyTable(7, '没有符合条件的订单', '可调整筛选条件或时间范围');

  const pager = pagerHtml(pageData ? pageData.total : 0, state.page, state.pageSize);
  holder.innerHTML = `<div class="table-wrap"><table class="data-table">
      <thead><tr>
        <th style="min-width:168px">订单号 / 下单时间</th>
        <th style="min-width:110px">收货人 / 手机号</th>
        <th>类型</th>
        <th style="width:100px">金额</th>
        <th style="width:92px">支付</th>
        <th style="width:96px">订单状态</th>
        <th style="min-width:268px">操作</th>
      </tr></thead><tbody>${rows}</tbody>
    </table></div>${pager.html}`;
  bindPager(holder, (page) => { state.page = page; loadList(); });
}

async function loadList() {
  if (state.loading) return;
  state.loading = true;
  const holder = $('#order-table');
  if (holder) holder.innerHTML = '<div class="table-empty">加载中…</div>';
  try {
    const data = await api.orderPage({
      page: state.page, pageSize: state.pageSize,
      number: state.number, phone: state.phone,
      status: state.status === '' ? undefined : Number(state.status),
      beginTime: state.beginTime, endTime: state.endTime
    });
    state.total = data ? data.total : 0;
    renderTable(data);
  } catch (error) {
    toastError(error);
    if (holder) holder.innerHTML = '<div class="table-empty">订单加载失败</div>';
  } finally {
    state.loading = false;
  }
}

function showDetail(id) {
  const drawer = openDrawer({
    title: '订单详情',
    subtitle: '加载中…',
    body: '<div class="empty-block">正在读取订单明细…</div>'
  });
  api.orderDetail(id).then((order) => {
    if (!order) return;
    drawer.el.querySelector('.drawer-header p').textContent = `订单号 ${order.number || order.id}`;
    const info = statusInfo(order.status);
    const items = Array.isArray(order.orderDetailList) ? order.orderDetailList : [];
    const itemsHtml = items.length ? `<div class="table-wrap"><table class="data-table">
      <thead><tr><th>商品</th><th>口味</th><th>数量</th><th class="right">金额</th></tr></thead>
      <tbody>${items.map((it) => `<tr>
        <td><div class="cell-strong">${escapeHtml(it.name || '')}</div>${it.setmealId ? '<span class="tag tag-info" style="margin-top:4px">套餐</span>' : ''}</td>
        <td class="muted">${escapeHtml(it.dishFlavor || '—')}</td>
        <td class="num">×${number(it.number)}</td>
        <td class="right num">${money(it.amount)}</td>
      </tr>`).join('')}</tbody>
    </table></div>` : `<div class="empty-block"><strong>无明细数据</strong><span class="text-sm">${escapeHtml(order.orderDishes || '后端未返回订单明细')}</span></div>`;

    drawer.body.innerHTML = `
      <div class="flex-between" style="margin-bottom:14px">
        <div><div style="font-size:13px;color:var(--muted)">订单状态</div><div style="font-size:22px;font-weight:700;margin-top:4px">${info.text}</div></div>
        <div style="text-align:right"><div style="font-size:13px;color:var(--muted)">实收金额</div><div style="font-size:26px;font-weight:700;margin-top:4px">${money(order.amount)}</div></div>
      </div>
      <div class="detail-list" style="margin-bottom:16px">
        <div class="detail-row"><span>订单号</span><strong class="mono">${escapeHtml(order.number || '—')}</strong></div>
        <div class="detail-row"><span>订单类型</span><strong>${order.orderType === 2 ? '堂食' : '外卖'}</strong></div>
        <div class="detail-row"><span>下单时间</span><strong>${escapeHtml(fmtDate(order.orderTime))}</strong></div>
        <div class="detail-row"><span>支付方式 / 状态</span><strong>${order.payMethod === 2 ? '支付宝' : order.payMethod === 1 ? '微信' : '—'} · ${PAY_STATUS[order.payStatus] || '—'}</strong></div>
        <div class="detail-row"><span>收货人</span><strong>${escapeHtml(order.consignee || order.userName || '—')} ${escapeHtml(order.phone || '')}</strong></div>
        <div class="detail-row"><span>地址</span><strong>${escapeHtml(order.address || '—')}</strong></div>
        <div class="detail-row"><span>备注</span><strong>${escapeHtml(order.remark || '—')}</strong></div>
        <div class="detail-row"><span>预计送达</span><strong>${escapeHtml(fmtDate(order.estimatedDeliveryTime))}</strong></div>
        <div class="detail-row"><span>打包费 / 餐具</span><strong>${money(order.packAmount || 0)} · ${number(order.tablewareNumber)} 份</strong></div>
        ${order.diningSessionId ? `<div class="detail-row"><span>堂食会话</span><strong>#${order.diningSessionId}</strong></div>` : ''}
        ${order.rejectionReason ? `<div class="detail-row"><span>拒单原因</span><strong>${escapeHtml(order.rejectionReason)}</strong></div>` : ''}
        ${order.cancelReason ? `<div class="detail-row"><span>取消原因</span><strong>${escapeHtml(order.cancelReason)}</strong></div>` : ''}
      </div>
      <div class="card" style="box-shadow:none">
        <div class="card-head" style="padding:14px 14px 10px"><h3 style="font-size:14px">订单明细</h3><span class="tag">${items.length} 项</span></div>
        <div class="card-body" style="padding:0 14px 14px">${itemsHtml}</div>
      </div>`;
  }).catch((error) => {
    drawer.body.innerHTML = `<div class="empty-block"><strong>读取失败</strong><span>${escapeHtml(error.message)}</span></div>`;
  });
}

async function doAction(action, id) {
  if (action === 'confirm') {
    const ok = await confirmDialog({ title: '确认接单', message: `确定接单订单 <strong>#${id}</strong> 吗？接单后可直接完成订单。`, confirmText: '接单' });
    if (!ok) return;
    try { await api.orderConfirm(id); toast('接单成功', 'success'); await refresh(); } catch (error) { toastError(error); }
    return;
  }
  if (action === 'complete') {
    const ok = await confirmDialog({ title: '完成订单', message: '确认订单已完成？完成后订单状态为「已完成」，支付状态将自动置为「已支付」。', confirmText: '完成订单' });
    if (!ok) return;
    try { await api.orderComplete(id); toast('订单已完成，支付状态已更新为已支付', 'success'); await refresh(); } catch (error) { toastError(error); }
    return;
  }
  if (action === 'mark-paid' || action === 'mark-unpaid') {
    const payStatus = action === 'mark-paid' ? 1 : 0;
    const ok = await confirmDialog({
      title: payStatus === 1 ? '标记已支付' : '标记未支付',
      message: `确定将订单 <strong>#${id}</strong> 的支付状态改为「${payStatus === 1 ? '已支付' : '未支付'}」吗？`,
      confirmText: payStatus === 1 ? '标记已支付' : '标记未支付'
    });
    if (!ok) return;
    try {
      await api.orderPayStatus(id, payStatus);
      toast(`订单已标记为${payStatus === 1 ? '已支付' : '未支付'}`, 'success');
      await refresh();
    } catch (error) { toastError(error); }
    return;
  }
  if (action === 'reject') {
    const reason = await promptDialog({ title: '拒单', label: '拒单原因', placeholder: '例如：菜品售罄、门店繁忙', required: true, multiline: true, danger: true, confirmText: '确认拒单' });
    if (!reason) return;
    try { await api.orderRejection({ id, rejectionReason: reason }); toast('已拒单', 'success'); await refresh(); } catch (error) { toastError(error); }
    return;
  }
  if (action === 'cancel') {
    const reason = await promptDialog({ title: '取消订单', label: '取消原因', placeholder: '请填写取消原因', required: true, multiline: true, danger: true, confirmText: '确认取消' });
    if (!reason) return;
    try { await api.orderCancel({ id, cancelReason: reason }); toast('订单已取消', 'success'); await refresh(); } catch (error) { toastError(error); }
  }
}

async function refresh() {
  await Promise.all([loadList(), loadStats(), loadAutoAccept()]);
}

export async function render(root) {
  state.page = 1;
  state.number = '';
  state.phone = '';
  state.status = '';
  state.beginTime = '';
  state.endTime = '';

  root.innerHTML = `
    <div class="section-stack">
      ${autoAcceptCard()}
      <div class="card"><div class="card-body card-body-pad"><div id="order-stats"><div class="text-sm muted">订单状态统计读取中…</div></div></div></div>
      <div class="card">
        <div class="toolbar">
          <div class="toolbar-left">
            <input id="f-number" class="input filter-input" placeholder="订单号" autocomplete="off">
            <input id="f-phone" class="input filter-input" placeholder="手机号" autocomplete="off">
            <select id="f-status" class="select filter-select">
              <option value="">全部状态</option>
              <option value="2">待接单</option><option value="3">已接单</option>
              <option value="5">已完成</option><option value="6">已取消</option><option value="1">待付款</option>
            </select>
            <input id="f-begin" class="input filter-date" type="datetime-local" title="下单开始时间">
            <span class="muted text-sm">至</span>
            <input id="f-end" class="input filter-date" type="datetime-local" title="下单结束时间">
          </div>
          <div class="toolbar-right">
            <button id="btn-search" class="btn btn-primary" type="button">查询</button>
            <button id="btn-reset" class="btn btn-ghost" type="button">重置</button>
          </div>
        </div>
        <div id="order-table"><div class="table-empty">加载中…</div></div>
      </div>
    </div>`;

  $('#btn-search').addEventListener('click', () => {
    state.page = 1;
    state.number = $('#f-number').value.trim();
    state.phone = $('#f-phone').value.trim();
    state.status = $('#f-status').value;
    state.beginTime = fromDateTimeInput($('#f-begin').value);
    state.endTime = fromDateTimeInput($('#f-end').value);
    loadList();
  });
  $('#btn-reset').addEventListener('click', () => {
    $('#f-number').value = ''; $('#f-phone').value = ''; $('#f-status').value = '';
    $('#f-begin').value = ''; $('#f-end').value = '';
    state.page = 1; state.number = ''; state.phone = ''; state.status = ''; state.beginTime = ''; state.endTime = '';
    loadList();
  });
  ['#f-number', '#f-phone'].forEach((sel) => {
    $(sel).addEventListener('keydown', (e) => { if (e.key === 'Enter') $('#btn-search').click(); });
  });
  $('#auto-accept-card').addEventListener('click', (event) => {
    const sw = event.target.closest('[data-auto]');
    if (!sw) return;
    void toggleAutoAccept(sw.dataset.auto);
  });
  $('#order-stats').addEventListener('click', (event) => {
    const btn = event.target.closest('[data-stat]');
    if (!btn) return;
    const status = btn.dataset.stat;
    state.status = state.status === status ? '' : status;
    $('#f-status').value = state.status;
    state.page = 1;
    loadList();
  });
  $('#order-table').addEventListener('click', (event) => {
    const btn = event.target.closest('[data-action]');
    if (!btn) return;
    const id = Number(btn.dataset.id);
    const action = btn.dataset.action;
    if (action === 'detail') showDetail(id);
    else doAction(action, id);
  });

  await refresh();
}

export function destroy() {}
