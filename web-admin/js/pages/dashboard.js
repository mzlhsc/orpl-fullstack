/* 工作台 */
import { api } from '../api.js';
import { $, escapeHtml, fmtDate, money, number, percent, shiftDate, splitCsv, todayISO } from '../util.js';
import { toastError, toast } from '../ui.js';
import { lineChart } from '../charts.js';

let chart = null;

const safe = (promise) => promise.catch((error) => ({ __error: error }));

function statCard(icon, label, value, sub, tone = '') {
  return `<div class="stat-card">
    <div class="stat-top"><span class="stat-ic ${tone}">${icon}</span><span>${escapeHtml(label)}</span></div>
    <div class="stat-value">${value}</div>
    <div class="stat-sub">${escapeHtml(sub || '')}</div>
  </div>`;
}

const ICON_MONEY = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M7 4h10M7 8h10M12 4v16M8 20l4-5 4 5"/></svg>';
const ICON_ORDER = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3.5h12v17l-2-1.4-2 1.4-2-1.4-2 1.4-2-1.4-2 1.4v-17Z"/><path d="M9 8h6M9 12h6"/></svg>';
const ICON_RATE = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="7" cy="7" r="2.4"/><circle cx="17" cy="17" r="2.4"/><path d="M6 18 18 6"/></svg>';
const ICON_PRICE = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M20 12l-8 8-9-9V4h7l10 8Z"/><circle cx="7.5" cy="7.5" r="1.4"/></svg>';
const ICON_USER = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="3.2"/><path d="M5 20c0-3.4 3-5.6 7-5.6s7 2.2 7 5.6"/></svg>';

export async function render(root) {
  const today = todayISO();
  root.innerHTML = `
    <div class="section-stack">
      <div class="stat-grid" id="stat-grid">
        ${statCard(ICON_MONEY, '今日营业额', '<span class="skeleton-text">—</span>', '已完成订单实收金额', '')}
        ${statCard(ICON_ORDER, '有效订单', '—', '今日已完成订单数', 'green')}
        ${statCard(ICON_RATE, '订单完成率', '—', '有效订单 / 总订单', 'blue')}
        ${statCard(ICON_PRICE, '平均客单价', '—', '营业额 / 有效订单', 'amber')}
        ${statCard(ICON_USER, '今日新增用户', '—', '注册用户数', 'slate')}
      </div>

      <div class="grid-2-1">
        <div class="card">
          <div class="card-head">
            <div><h2>近 7 天营业额</h2><p>${escapeHtml(shiftDate(-6))} 至 ${escapeHtml(today)} · 已完成订单</p></div>
            <div class="card-head-actions">
              <div class="chart-legend"><span class="legend-item"><span class="legend-swatch" style="background:#e8552d"></span>营业额</span></div>
            </div>
          </div>
          <div class="card-body">
            <div class="chart-box" style="height:296px"><canvas id="dash-turnover"></canvas></div>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <div><h2>门店状态</h2><p>营业状态会同步到用户端</p></div>
            <button class="switch brand" id="dash-shop-switch" type="button" aria-label="切换营业状态"></button>
          </div>
          <div class="card-body">
            <div class="flex-between" style="padding:2px 0 12px">
              <div>
                <div style="font-size:20px;font-weight:700" id="shop-state-text">读取中…</div>
                <div class="text-sm muted" id="shop-state-sub">正在读取 Redis 中的营业状态</div>
              </div>
              <span class="tag" id="shop-state-tag">—</span>
            </div>
            <div class="overview-list" id="order-overview">
              <div class="overview-row"><span>全部订单</span><strong>—</strong></div>
              <div class="overview-row"><span>已完成</span><strong>—</strong></div>
              <div class="overview-row"><span>已取消</span><strong>—</strong></div>
              <div class="overview-row"><span>待接单</span><strong>—</strong></div>
              <div class="overview-row"><span>待处理</span><strong>—</strong></div>
            </div>
          </div>
        </div>
      </div>

      <div class="grid-2">
        <div class="card">
          <div class="card-head">
            <div><h2>待接单订单</h2><p>点击接单即流转为「已接单」</p></div>
            <div class="card-head-actions"><a class="btn btn-ghost btn-sm" href="#/orders">全部订单</a></div>
          </div>
          <div class="card-body"><div class="quick-list" id="pending-orders"><div class="empty-block">加载中…</div></div></div>
        </div>

        <div class="card">
          <div class="card-head"><div><h2>商品总览</h2><p>菜品与套餐的起售、停售数量</p></div></div>
          <div class="card-body">
            <div class="grid-2" style="gap:10px">
              <div class="mini-stat"><span class="mini-stat-label">菜品在售</span><span class="mini-stat-value" id="dish-sold">—</span></div>
              <div class="mini-stat"><span class="mini-stat-label">菜品停售</span><span class="mini-stat-value" id="dish-stop">—</span></div>
              <div class="mini-stat"><span class="mini-stat-label">套餐在售</span><span class="mini-stat-value" id="setmeal-sold">—</span></div>
              <div class="mini-stat"><span class="mini-stat-label">套餐停售</span><span class="mini-stat-value" id="setmeal-stop">—</span></div>
            </div>
            <div class="flex gap-8 mt-16">
              <a class="btn btn-ghost btn-sm" href="#/dishes">管理菜品</a>
              <a class="btn btn-ghost btn-sm" href="#/setmeals">管理套餐</a>
            </div>
          </div>
        </div>
      </div>
    </div>`;

  const [business, orderOverview, dishOverview, setmealOverview, shopStatus, orderStats, recentOrders, turnover] = await Promise.all([
    safe(api.businessData()),
    safe(api.overviewOrders()),
    safe(api.overviewDishes()),
    safe(api.overviewSetmeals()),
    safe(api.shopStatus()),
    safe(api.orderStatistics()),
    safe(api.orderPage({ page: 1, pageSize: 6, status: 2 })),
    safe(api.reportTurnover(shiftDate(-6), today))
  ]);

  const bd = business.__error ? null : business;
  if (bd) {
    const grid = $('#stat-grid');
    grid.innerHTML = [
      statCard(ICON_MONEY, '今日营业额', money(bd.turnover), '已完成订单实收金额'),
      statCard(ICON_ORDER, '有效订单', `${number(bd.validOrderCount)}<small>单</small>`, '今日已完成订单数', 'green'),
      statCard(ICON_RATE, '订单完成率', percent(bd.orderCompletionRate), '有效订单 / 总订单', 'blue'),
      statCard(ICON_PRICE, '平均客单价', money(bd.unitPrice), '营业额 / 有效订单', 'amber'),
      statCard(ICON_USER, '今日新增用户', `${number(bd.newUsers)}<small>人</small>`, '注册用户数', 'slate')
    ].join('');
  } else {
    toastError(business.__error);
  }

  if (!orderOverview.__error) {
    const oo = orderOverview;
    $('#order-overview').innerHTML = `
      <div class="overview-row"><span>全部订单</span><strong>${number(oo.allOrders)}</strong></div>
      <div class="overview-row"><span>已完成</span><strong>${number(oo.completedOrders)}</strong></div>
      <div class="overview-row"><span>已取消</span><strong>${number(oo.cancelledOrders)}</strong></div>
      <div class="overview-row"><span>待接单</span><strong style="color:var(--brand-strong)">${number(oo.waitingOrders)}</strong></div>
      <div class="overview-row"><span>待处理</span><strong>${number(oo.deliveredOrders)}</strong></div>`;
  }

  if (!dishOverview.__error) {
    $('#dish-sold').textContent = number(dishOverview.sold);
    $('#dish-stop').textContent = number(dishOverview.discontinued);
  }
  if (!setmealOverview.__error) {
    $('#setmeal-sold').textContent = number(setmealOverview.sold);
    $('#setmeal-stop').textContent = number(setmealOverview.discontinued);
  }

  // 店铺状态
  const shopSwitch = $('#dash-shop-switch');
  let shopOn = false;
  const paintShop = () => {
    shopSwitch.classList.toggle('on', shopOn);
    $('#shop-state-text').textContent = shopOn ? '营业中' : '打烊中';
    $('#shop-state-sub').textContent = shopOn ? '用户端可以正常下单' : '用户端将提示门店休息';
    const tag = $('#shop-state-tag');
    tag.className = `tag ${shopOn ? 'tag-success' : 'tag-danger'}`;
    tag.innerHTML = `<span class="tag-dot"></span>${shopOn ? '营业' : '打烊'}`;
  };
  if (!shopStatus.__error) {
    shopOn = Number(shopStatus) === 1;
    paintShop();
  }
  shopSwitch.addEventListener('click', async () => {
    if (shopSwitch.disabled) return;
    shopSwitch.disabled = true;
    try {
      const next = shopOn ? 0 : 1;
      await api.shopSet(next);
      shopOn = next === 1;
      paintShop();
      window.dispatchEvent(new CustomEvent('op:shop-refresh'));
      toast(shopOn ? '店铺已开始营业' : '店铺已打烊', 'success');
    } catch (error) {
      toastError(error);
    } finally {
      shopSwitch.disabled = false;
    }
  });

  // 近 7 天营业额
  if (!turnover.__error) {
    const labels = splitCsv(turnover.dateList).map((d) => d.slice(5));
    const values = splitCsv(turnover.turnoverList).map((v) => Number(v) || 0);
    const canvas = $('#dash-turnover');
    if (canvas) chart = lineChart(canvas, { labels, series: [{ name: '营业额', data: values, color: '#e8552d' }], yFormat: (v) => money(v) });
  }

  // 待接单
  const pendingBox = $('#pending-orders');
  if (!recentOrders.__error) {
    const list = (recentOrders && recentOrders.list) || [];
    if (!list.length) {
      pendingBox.innerHTML = `<div class="empty-block">
        <svg viewBox="0 0 64 64" width="46" height="46" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 10h24v44l-4-3-4 3-4-3-4 3-4-3-4 3V10Z"/><path d="M25 22h14M25 30h14M25 38h8" stroke-dasharray="3 4"/></svg>
        <strong>没有待接单订单</strong><span>新订单会出现在这里</span></div>`;
    } else {
      pendingBox.innerHTML = list.map((o) => `
        <div class="quick-order">
          <div class="quick-order-main">
            <div class="quick-order-no">${escapeHtml(o.number || `#${o.id}`)}</div>
            <div class="quick-order-sub">${escapeHtml(o.orderType === 2 ? '堂食' : '外卖')} · ${escapeHtml(o.consignee || o.userName || '顾客')} · ${escapeHtml(fmtDate(o.orderTime))}</div>
          </div>
          <div class="flex gap-8">
            <span class="quick-order-amount">${money(o.amount)}</span>
            <button class="btn btn-primary btn-xs" data-accept="${o.id}">接单</button>
          </div>
        </div>`).join('');
      pendingBox.addEventListener('click', async (event) => {
        const btn = event.target.closest('[data-accept]');
        if (!btn) return;
        btn.disabled = true;
        try {
          await api.orderConfirm(Number(btn.dataset.accept));
          toast('接单成功', 'success');
          btn.closest('.quick-order').remove();
          if (!pendingBox.children.length) {
            pendingBox.innerHTML = '<div class="empty-block"><strong>没有待接单订单</strong><span>新订单会出现在这里</span></div>';
          }
        } catch (error) {
          toastError(error);
          btn.disabled = false;
        }
      });
    }
  } else {
    pendingBox.innerHTML = '<div class="empty-block"><strong>待接单订单读取失败</strong><span>请确认后端服务</span></div>';
  }

  if (orderStats.__error) console.warn(orderStats.__error);
}

export function destroy() {
  if (chart) { chart.destroy(); chart = null; }
}
