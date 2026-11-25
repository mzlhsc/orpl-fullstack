/* 数据统计 */
import { api } from '../api.js';
import { $, downloadBlob, escapeHtml, money, number, percent, shiftDate, splitCsv, todayISO } from '../util.js';
import { setBtnLoading, toast, toastError } from '../ui.js';
import { barChart, lineChart } from '../charts.js';

let charts = [];
const state = { begin: shiftDate(-6), end: todayISO() };

function destroyCharts() {
  charts.forEach((c) => { try { c.destroy(); } catch (e) { console.warn(e); } });
  charts = [];
}

function topList(top10) {
  const names = splitCsv(top10?.nameList);
  const nums = splitCsv(top10?.numberList).map((n) => Number(n) || 0);
  if (!names.length) return '<div class="empty-block"><strong>暂无销量数据</strong><span>所选区间内还没有已完成订单</span></div>';
  const max = Math.max(...nums, 1);
  return `<div class="top-list">${names.map((name, i) => `
    <div class="top-row">
      <span class="top-rank ${i === 0 ? 'top1' : i === 1 ? 'top2' : i === 2 ? 'top3' : ''}">${i + 1}</span>
      <div>
        <div class="flex-between" style="margin-bottom:4px"><span class="ellipsis" style="max-width:190px">${escapeHtml(name)}</span><span class="top-num">${number(nums[i] || 0)} 份</span></div>
        <div class="top-bar-track"><div class="top-bar-fill" style="width:${Math.round(((nums[i] || 0) / max) * 100)}%"></div></div>
      </div>
      <span></span>
    </div>`).join('')}</div>`;
}

async function loadAll() {
  destroyCharts();
  const [turnover, users, orders, top10] = await Promise.all([
    api.reportTurnover(state.begin, state.end).catch((e) => ({ __error: e })),
    api.reportUsers(state.begin, state.end).catch((e) => ({ __error: e })),
    api.reportOrders(state.begin, state.end).catch((e) => ({ __error: e })),
    api.reportTop10(state.begin, state.end).catch((e) => ({ __error: e }))
  ]);

  if (orders.__error) toastError(orders.__error);
  else {
    $('#rep-total-orders').textContent = number(orders.totalOrderCount);
    $('#rep-valid-orders').textContent = number(orders.validOrderCount);
    $('#rep-rate').textContent = percent(orders.orderCompletionRate);
  }

  if (!turnover.__error) {
    const labels = splitCsv(turnover.dateList).map((d) => d.slice(5));
    const values = splitCsv(turnover.turnoverList).map((v) => Number(v) || 0);
    charts.push(lineChart($('#chart-turnover'), { labels, series: [{ name: '营业额', data: values, color: '#e8552d' }], yFormat: (v) => money(v) }));
  } else {
    toastError(turnover.__error);
  }

  if (!users.__error) {
    const labels = splitCsv(users.dateList).map((d) => d.slice(5));
    charts.push(lineChart($('#chart-users'), {
      labels,
      series: [
        { name: '总用户量', data: splitCsv(users.totalUserList).map((v) => Number(v) || 0), color: '#2f6f8f', area: false },
        { name: '新增用户', data: splitCsv(users.newUserList).map((v) => Number(v) || 0), color: '#e8552d', area: false }
      ],
      yFormat: (v) => number(v)
    }));
  } else {
    toastError(users.__error);
  }

  if (!orders.__error) {
    const labels = splitCsv(orders.dateList).map((d) => d.slice(5));
    charts.push(barChart($('#chart-orders'), {
      labels,
      series: [
        { name: '订单总数', data: splitCsv(orders.orderCountList).map((v) => Number(v) || 0), color: '#9fb3c8' },
        { name: '有效订单', data: splitCsv(orders.validOrderCountList).map((v) => Number(v) || 0), color: '#e8552d' }
      ],
      yFormat: (v) => number(v)
    }));
  }

  $('#top10-box').innerHTML = top10.__error
    ? `<div class="empty-block"><strong>读取失败</strong><span>${escapeHtml(top10.__error.message)}</span></div>`
    : topList(top10);
}

export async function render(root) {
  root.innerHTML = `
    <div class="section-stack">
      <div class="card">
        <div class="toolbar">
          <div class="toolbar-left">
            <input id="rep-begin" class="input filter-date" type="date" value="${escapeHtml(state.begin)}">
            <span class="muted text-sm">至</span>
            <input id="rep-end" class="input filter-date" type="date" value="${escapeHtml(state.end)}">
            <button id="rep-search" class="btn btn-primary" type="button">查询</button>
            <button id="rep-7" class="btn btn-ghost" type="button">近 7 天</button>
            <button id="rep-30" class="btn btn-ghost" type="button">近 30 天</button>
          </div>
          <div class="toolbar-right">
            <button id="rep-export" class="btn btn-soft" type="button">
              <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3v12M7 10l5 5 5-5"/><path d="M5 19h14"/></svg>导出运营报表
            </button>
          </div>
        </div>
        <div class="card-body card-body-pad">
          <div class="stat-grid" style="grid-template-columns:repeat(3,minmax(0,1fr))">
            <div class="stat-card"><div class="stat-top"><span class="stat-ic">单</span>订单总数</div><div class="stat-value" id="rep-total-orders">—</div><div class="stat-sub">所选区间全部订单</div></div>
            <div class="stat-card"><div class="stat-top"><span class="stat-ic green">完</span>有效订单</div><div class="stat-value" id="rep-valid-orders">—</div><div class="stat-sub">状态为已完成</div></div>
            <div class="stat-card"><div class="stat-top"><span class="stat-ic blue">率</span>订单完成率</div><div class="stat-value" id="rep-rate">—</div><div class="stat-sub">有效订单 / 订单总数</div></div>
          </div>
        </div>
      </div>

      <div class="report-grid">
        <div class="card">
          <div class="card-head"><div><h2>营业额趋势</h2><p>按天统计已完成订单金额</p></div>
            <div class="chart-legend"><span class="legend-item"><span class="legend-swatch" style="background:#e8552d"></span>营业额</span></div>
          </div>
          <div class="card-body"><div class="chart-box"><canvas id="chart-turnover"></canvas></div></div>
        </div>
        <div class="card">
          <div class="card-head"><div><h2>用户增长</h2><p>总用户量与每日新增</p></div>
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-swatch" style="background:#2f6f8f"></span>总用户量</span>
              <span class="legend-item"><span class="legend-swatch" style="background:#e8552d"></span>新增用户</span>
            </div>
          </div>
          <div class="card-body"><div class="chart-box"><canvas id="chart-users"></canvas></div></div>
        </div>
        <div class="card">
          <div class="card-head"><div><h2>订单统计</h2><p>每日订单总数与有效订单</p></div>
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-swatch" style="background:#9fb3c8"></span>订单总数</span>
              <span class="legend-item"><span class="legend-swatch" style="background:#e8552d"></span>有效订单</span>
            </div>
          </div>
          <div class="card-body"><div class="chart-box"><canvas id="chart-orders"></canvas></div></div>
        </div>
        <div class="card">
          <div class="card-head"><div><h2>销量 Top 10</h2><p>所选区间内商品销量排名</p></div></div>
          <div class="card-body" id="top10-box"><div class="empty-block">加载中…</div></div>
        </div>
      </div>
    </div>`;

  $('#rep-search').addEventListener('click', () => {
    const begin = $('#rep-begin').value;
    const end = $('#rep-end').value;
    if (!begin || !end) { toast('请选择开始和结束日期', 'warn'); return; }
    if (begin > end) { toast('开始日期不能晚于结束日期', 'warn'); return; }
    state.begin = begin; state.end = end;
    loadAll();
  });
  $('#rep-7').addEventListener('click', () => {
    state.begin = shiftDate(-6); state.end = todayISO();
    $('#rep-begin').value = state.begin; $('#rep-end').value = state.end;
    loadAll();
  });
  $('#rep-30').addEventListener('click', () => {
    state.begin = shiftDate(-29); state.end = todayISO();
    $('#rep-begin').value = state.begin; $('#rep-end').value = state.end;
    loadAll();
  });
  $('#rep-export').addEventListener('click', async (event) => {
    const btn = event.currentTarget;
    setBtnLoading(btn, true, '导出中…');
    try {
      const { blob, filename } = await api.reportExport();
      downloadBlob(blob, filename);
      toast('报表已开始下载', 'success');
    } catch (error) {
      toastError(error);
    } finally {
      setBtnLoading(btn, false);
    }
  });

  await loadAll();
}

export function destroy() { destroyCharts(); }
