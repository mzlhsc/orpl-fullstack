/* 应用入口：登录、路由、侧边栏、店铺状态与账号操作 */
import { api, auth, ApiError } from './api.js';
import { $, escapeHtml, setLoading } from './util.js';
import { toast, toastError, openModal, setBtnLoading } from './ui.js';

const ICONS = {
  dashboard: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3.5" y="3.5" width="7" height="7" rx="1.6"/><rect x="13.5" y="3.5" width="7" height="7" rx="1.6"/><rect x="3.5" y="13.5" width="7" height="7" rx="1.6"/><rect x="13.5" y="13.5" width="7" height="7" rx="1.6"/></svg>',
  orders: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M6 3.5h12v17l-2-1.4-2 1.4-2-1.4-2 1.4-2-1.4-2 1.4v-17Z"/><path d="M9 8h6M9 12h6M9 16h3"/></svg>',
  dishes: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 11h16a8 8 0 0 1-8 8 8 8 0 0 1-8-8Z"/><path d="M6 7c0-1.6 1.3-2.8 3-2.8M12 7c0-2.1 1.6-3.5 3.6-3.5"/><path d="M9 22h6"/></svg>',
  setmeal: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 8.5 12 4l8 4.5v7L12 20l-8-4.5v-7Z"/><path d="M4 8.5 12 13l8-4.5M12 13V20"/></svg>',
  category: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M12 3.5 21 8l-9 4.5L3 8l9-4.5Z"/><path d="M3 12l9 4.5 9-4.5"/><path d="M3 16l9 4.5 9-4.5"/></svg>',
  employees: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="9" cy="8" r="3.2"/><path d="M3 20c0-3 2.7-5 6-5s6 2 6 5"/><circle cx="17.5" cy="9" r="2.4"/><path d="M15.5 14.6c2.7-.4 5.5 1.2 5.5 4.4"/></svg>',
  dining: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="9" width="18" height="10" rx="2"/><path d="M6 9V6M18 9V6M8 19v2M16 19v2"/><path d="M8 13h8"/></svg>',
  reports: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 19h16"/><rect x="5" y="12" width="3.5" height="6"/><rect x="10.5" y="8" width="3.5" height="10"/><rect x="16" y="4" width="3.5" height="14"/></svg>',
  settings: '<svg class="nav-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3h.1a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5h.1a1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9v.1a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1Z"/></svg>'
};

const ROUTES = {
  dashboard: { title: '工作台', subtitle: '今日经营概览与快捷处理', icon: 'dashboard', module: './pages/dashboard.js', group: '经营' },
  orders: { title: '订单管理', subtitle: '外卖与堂食订单的接单、支付与结算', icon: 'orders', module: './pages/orders.js', group: '经营' },
  dining: { title: '堂食管理', subtitle: '桌台、预约、会话与堂食报表', icon: 'dining', module: './pages/dining.js', group: '经营' },
  dishes: { title: '菜品管理', subtitle: '菜品、库存、口味与起售停售', icon: 'dishes', module: './pages/dishes.js', group: '商品' },
  setmeals: { title: '套餐管理', subtitle: '套餐配置与套餐菜品维护', icon: 'setmeal', module: './pages/setmeals.js', group: '商品' },
  categories: { title: '分类管理', subtitle: '菜品分类与套餐分类', icon: 'category', module: './pages/categories.js', group: '商品' },
  employees: { title: '员工管理', subtitle: '员工账号、状态与密码', icon: 'employees', module: './pages/employees.js', group: '组织' },
  reports: { title: '数据统计', subtitle: '营业额、用户、订单与销量排行', icon: 'reports', module: './pages/reports.js', group: '分析' },
  settings: { title: '店铺设置', subtitle: '营业状态与账号安全', icon: 'settings', module: './pages/settings.js', group: '设置' }
};

const GROUP_ORDER = ['经营', '商品', '组织', '分析', '设置'];
let currentPage = null;
let currentModule = null;
let shopOn = false;
let shopLoaded = false;

function buildNav() {
  const nav = $('#sidebar-nav');
  const groups = {};
  Object.entries(ROUTES).forEach(([key, route]) => {
    (groups[route.group] ||= []).push({ key, ...route });
  });
  nav.innerHTML = GROUP_ORDER.filter((g) => groups[g]).map((group) => `
    <div class="nav-group-title">${escapeHtml(group)}</div>
    ${groups[group].map((item) => `
      <div class="nav-item" data-route="${item.key}" role="button" tabindex="0">
        ${ICONS[item.icon] || ''}
        <span class="nav-text">${escapeHtml(item.title)}</span>
        ${item.key === 'orders' ? '<span class="nav-badge" id="nav-order-badge" hidden>0</span>' : ''}
      </div>`).join('')}
  `).join('');
  nav.addEventListener('click', (event) => {
    const item = event.target.closest('[data-route]');
    if (!item) return;
    location.hash = `#/${item.dataset.route}`;
  });
  nav.addEventListener('keydown', (event) => {
    if (event.key !== 'Enter' && event.key !== ' ') return;
    const item = event.target.closest('[data-route]');
    if (!item) return;
    event.preventDefault();
    location.hash = `#/${item.dataset.route}`;
  });
}

function setActiveNav(routeKey) {
  document.querySelectorAll('.nav-item').forEach((el) => {
    el.classList.toggle('active', el.dataset.route === routeKey);
  });
  const route = ROUTES[routeKey] || ROUTES.dashboard;
  $('#page-title').textContent = route.title;
  $('#page-subtitle').textContent = route.subtitle;
}

function updateUserChip() {
  const user = auth.user || {};
  const name = user.name || user.userName || '管理员';
  $('#user-name').textContent = name;
  $('#user-account').textContent = user.userName || 'admin';
  $('#user-avatar').textContent = (name || '管').trim().slice(0, 1);
}

function paintShopStatus() {
  const pill = $('#shop-toggle');
  const label = pill.querySelector('.shop-pill-text');
  const dot = pill.querySelector('.status-dot');
  const sideDot = $('#side-shop-dot');
  const sideText = $('#side-shop-text');
  pill.classList.toggle('on', shopOn);
  pill.classList.toggle('off', !shopOn);
  dot.className = `status-dot ${shopOn ? 'ok' : 'off'}`;
  label.textContent = shopOn ? '营业中' : '打烊中';
  pill.title = shopOn ? '点击切换为打烊' : '点击切换为营业';
  sideDot.className = `status-dot ${shopOn ? 'ok' : 'off'}`;
  sideText.textContent = shopOn ? '店铺营业中' : '店铺已打烊';
}

async function refreshShopStatus(silent = true) {
  try {
    const status = await api.shopStatus();
    shopOn = Number(status) === 1;
    shopLoaded = true;
    paintShopStatus();
    return shopOn;
  } catch (error) {
    if (!silent) toastError(error);
    shopLoaded = false;
    $('#side-shop-text').textContent = '店铺状态读取失败';
    return null;
  }
}

async function toggleShop() {
  const btn = $('#shop-toggle');
  if (btn.disabled) return;
  btn.disabled = true;
  const next = shopOn ? 0 : 1;
  try {
    await api.shopSet(next);
    shopOn = next === 1;
    paintShopStatus();
    toast(shopOn ? '店铺已切换为营业中' : '店铺已切换为打烊中', 'success');
    window.dispatchEvent(new CustomEvent('op:shop-changed', { detail: { status: shopOn ? 1 : 0 } }));
  } catch (error) {
    toastError(error);
  } finally {
    btn.disabled = false;
  }
}

export function navigate(routeKey) {
  if (!ROUTES[routeKey]) routeKey = 'dashboard';
  location.hash = `#/${routeKey}`;
}

async function loadRoute(routeKey) {
  const route = ROUTES[routeKey];
  if (!route) { navigate('dashboard'); return; }
  setActiveNav(routeKey);
  const pageEl = $('#page');
  if (currentModule && typeof currentModule.destroy === 'function') {
    try { currentModule.destroy(); } catch (e) { console.warn(e); }
  }
  currentModule = null;
  pageEl.innerHTML = '';
  setLoading(true, '正在加载页面…');
  try {
    const module = await import(route.module);
    currentModule = module;
    currentPage = routeKey;
    await module.render(pageEl);
  } catch (error) {
    console.error(error);
    pageEl.innerHTML = `<div class="card"><div class="empty-block">
      <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.6"><circle cx="12" cy="12" r="9"/><path d="M12 7v6M12 16v.01"/></svg>
      <strong>页面加载失败</strong>
      <span>${escapeHtml(error.message || '未知错误')}</span>
    </div></div>`;
  } finally {
    setLoading(false);
  }
}

function handleHash() {
  const raw = location.hash.replace(/^#\/?/, '');
  const key = raw.split('?')[0] || 'dashboard';
  if (!auth.token) {
    showLogin();
    return;
  }
  loadRoute(key);
}

function showLogin() {
  $('#login-view').hidden = false;
  $('#app-view').hidden = true;
  const username = $('#login-username');
  if (username) setTimeout(() => username.focus(), 60);
}

function showApp() {
  $('#login-view').hidden = true;
  $('#app-view').hidden = false;
  updateUserChip();
  refreshShopStatus(true);
}

async function enterApp() {
  showApp();
  if (!location.hash || location.hash === '#') location.hash = '#/dashboard';
  else handleHash();
}

function bindLogin() {
  const form = $('#login-form');
  const errorEl = $('#login-error');
  const btn = $('#login-btn');
  const password = $('#login-password');
  const username = $('#login-username');

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    errorEl.hidden = true;
    const u = username.value.trim();
    const p = password.value;
    if (!u || !p) {
      errorEl.hidden = false;
      errorEl.textContent = '请输入用户名和密码';
      return;
    }
    setBtnLoading(btn, true, '登录中…');
    try {
      const vo = await api.login(u, p);
      if (!vo || !vo.token) throw new ApiError('登录返回异常：未获取到令牌');
      auth.setSession(vo);
      password.value = '';
      toast(`欢迎回来，${vo.name || vo.userName || '管理员'}`, 'success');
      await enterApp();
    } catch (error) {
      errorEl.hidden = false;
      errorEl.textContent = error.message || '登录失败';
      password.focus();
      password.select();
    } finally {
      setBtnLoading(btn, false);
    }
  });
}

function bindSidebar() {
  const sidebar = $('#sidebar');
  const mask = $('#sidebar-mask');
  const open = () => {
    sidebar.classList.add('open');
    mask.hidden = false;
  };
  const close = () => {
    sidebar.classList.remove('open');
    mask.hidden = true;
  };
  $('#sidebar-open').addEventListener('click', open);
  $('#sidebar-close').addEventListener('click', close);
  mask.addEventListener('click', close);
  window.addEventListener('hashchange', close);
}

function bindAccount() {
  $('#logout-btn').addEventListener('click', async () => {
    try { await api.logout(); } catch (e) { /* 令牌失效也继续退出 */ }
    auth.clear();
    toast('已退出登录', 'info');
    showLogin();
  });
  $('#user-chip').addEventListener('click', openPasswordModal);
}

function openPasswordModal() {
  const user = auth.user || {};
  openModal({
    title: '修改登录密码',
    subtitle: '修改成功后下次登录请使用新密码',
    body: `<form id="pwd-form" class="form-grid" autocomplete="off">
      <div class="form-item span-2"><label>当前密码<em>*</em></label><input class="input" type="password" name="oldPassword" placeholder="请输入当前密码" autocomplete="current-password"></div>
      <div class="form-item span-2"><label>新密码<em>*</em></label><input class="input" type="password" name="newPassword" placeholder="至少 6 位，不能与原密码相同" autocomplete="new-password"></div>
      <div class="form-item span-2"><label>确认新密码<em>*</em></label><input class="input" type="password" name="confirmPassword" placeholder="再次输入新密码" autocomplete="new-password"></div>
      <p class="form-error" data-pwd-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button>
             <button class="btn btn-primary" type="button" data-save>保存新密码</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#pwd-form');
      const errorEl = modal.querySelector('[data-pwd-error]');
      const saveBtn = modal.querySelector('[data-save]');
      const submit = async () => {
        errorEl.hidden = true;
        const oldPassword = form.oldPassword.value;
        const newPassword = form.newPassword.value;
        const confirmPassword = form.confirmPassword.value;
        if (!oldPassword || !newPassword) {
          errorEl.hidden = false; errorEl.textContent = '请填写当前密码与新密码'; return;
        }
        if (newPassword.length < 6) {
          errorEl.hidden = false; errorEl.textContent = '新密码至少 6 位'; return;
        }
        if (newPassword !== confirmPassword) {
          errorEl.hidden = false; errorEl.textContent = '两次输入的新密码不一致'; return;
        }
        if (newPassword === oldPassword) {
          errorEl.hidden = false; errorEl.textContent = '新密码不能与当前密码相同'; return;
        }
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          await api.employeePassword({ empId: user.id, oldPassword, newPassword });
          toast('密码修改成功', 'success');
          close();
        } catch (error) {
          errorEl.hidden = false; errorEl.textContent = error.message || '修改失败';
        } finally {
          setBtnLoading(saveBtn, false);
        }
      };
      saveBtn.addEventListener('click', submit);
      form.addEventListener('submit', (e) => { e.preventDefault(); submit(); });
    }
  });
}

function bindGlobalEvents() {
  window.addEventListener('op:unauthorized', () => {
    auth.clear();
    setLoading(false);
    showLogin();
    toast('登录状态已失效，请重新登录', 'warn');
  });
  window.addEventListener('op:shop-refresh', () => refreshShopStatus(true));
  window.addEventListener('op:shop-changed', () => { /* 页面自行刷新 */ });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      const root = document.getElementById('modal-root');
      if (root && root.children.length) {
        const closeBtn = root.lastElementChild.querySelector('[data-close]');
        if (closeBtn) closeBtn.click();
      }
    }
  });
}

async function bootstrap() {
  buildNav();
  bindLogin();
  bindSidebar();
  bindAccount();
  bindGlobalEvents();
  $('#shop-toggle').addEventListener('click', toggleShop);

  if (auth.token) {
    showApp();
    updateUserChip();
    if (!location.hash || location.hash === '#') location.hash = '#/dashboard';
    else handleHash();
    refreshShopStatus(true);
  } else {
    showLogin();
  }
  window.addEventListener('hashchange', handleHash);
}

bootstrap().catch((error) => {
  console.error(error);
  toast('管理端初始化失败：' + (error.message || error), 'error', 5000);
});

export { refreshShopStatus, navigate as go };
