/* 与后端 /admin/** 对接的 API 封装（经 nginx /api/ 反向代理） */
const BASE = '/api';
const TOKEN_KEY = 'op_admin_token';
const USER_KEY = 'op_admin_user';

export class ApiError extends Error {
  constructor(message, code = 0, status = 0, payload = null) {
    super(message || '请求失败');
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
    this.payload = payload;
  }
}

export const auth = {
  get token() { return localStorage.getItem(TOKEN_KEY) || ''; },
  get user() {
    try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch { return null; }
  },
  setSession(loginVO) {
    if (loginVO && loginVO.token) localStorage.setItem(TOKEN_KEY, loginVO.token);
    if (loginVO) localStorage.setItem(USER_KEY, JSON.stringify(loginVO));
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
};

function buildQuery(query) {
  if (!query) return '';
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === null || value === undefined || value === '') return;
    if (Array.isArray(value)) {
      value.forEach((v) => params.append(key, v));
    } else {
      params.append(key, value);
    }
  });
  const s = params.toString();
  return s ? `?${s}` : '';
}

function fullPath(path, query) {
  const clean = path.startsWith('/') ? path : `/${path}`;
  return BASE + clean + buildQuery(query);
}

async function parseBody(response) {
  const type = response.headers.get('content-type') || '';
  if (type.includes('application/json')) {
    const text = await response.text();
    if (!text) return null;
    try { return JSON.parse(text); } catch { return text; }
  }
  const text = await response.text();
  return text || null;
}

async function request(path, options = {}) {
  const { method = 'GET', query, body, form, headers = {}, signal, raw = false } = options;
  const url = fullPath(path, query);
  const finalHeaders = { Accept: 'application/json', ...headers };
  let payload;
  if (form) {
    payload = form; // FormData 由浏览器自动设置 boundary
  } else if (body !== undefined) {
    finalHeaders['Content-Type'] = 'application/json;charset=UTF-8';
    payload = JSON.stringify(body);
  }
  const token = auth.token;
  if (token) finalHeaders['zmlhsc'] = token;

  let response;
  try {
    response = await fetch(url, { method, headers: finalHeaders, body: payload, signal });
  } catch (e) {
    if (e.name === 'AbortError') throw e;
    throw new ApiError('网络异常，请确认后端服务与 nginx 代理已启动', 0, 0);
  }

  if (raw) {
    if (!response.ok) {
      const data = await parseBody(response).catch(() => null);
      throw new ApiError(data && data.msg ? data.msg : `下载失败（HTTP ${response.status}）`, data && data.code, response.status, data);
    }
    return response;
  }

  const data = await parseBody(response);

  if (!response.ok) {
    if (response.status === 401) {
      auth.clear();
      window.dispatchEvent(new CustomEvent('op:unauthorized'));
    }
    const msg = (data && (data.msg || data.message)) || httpMessage(response.status);
    throw new ApiError(msg, data && data.code, response.status, data);
  }

  // 统一结构 R<T>
  if (data && typeof data === 'object' && 'code' in data && ('msg' in data || 'data' in data)) {
    if (Number(data.code) !== 200) {
      throw new ApiError(data.msg || '业务处理失败', data.code, response.status, data);
    }
    return data.data;
  }
  return data;
}

function httpMessage(status) {
  const map = {
    400: '请求参数有误', 401: '登录状态已失效，请重新登录', 403: '没有权限访问',
    404: '请求的资源不存在', 405: '请求方式不支持', 500: '服务器内部异常',
    502: '网关错误，请确认后端服务是否启动', 503: '服务暂不可用', 504: '网关超时'
  };
  return map[status] || `请求失败（HTTP ${status}）`;
}

export const api = {
  request,
  download: async (path, filename, query) => {
    const response = await request(path, { raw: true, query });
    const blob = await response.blob();
    const disposition = response.headers.get('content-disposition') || '';
    let name = filename;
    const match = disposition.match(/filename=([^;]+)/i);
    if (match && match[1]) name = match[1].trim().replace(/["']/g, '');
    return { blob, filename: name || filename };
  },

  /* ---- 员工 ---- */
  login: (username, password) => request('/employee/login', { method: 'POST', body: { username, password } }),
  logout: () => request('/employee/logout', { method: 'POST' }),
  employeePage: (query) => request('/employee/page', { query }),
  employeeGet: (id) => request(`/employee/${id}`),
  employeeSave: (body) => request('/employee', { method: 'POST', body }),
  employeeUpdate: (body) => request('/employee', { method: 'PUT', body }),
  employeePassword: (body) => request('/employee/editPassword', { method: 'PUT', body }),
  employeeStatus: (status, id) => request(`/employee/status/${status}`, { method: 'POST', query: { id } }),

  /* ---- 分类 ---- */
  categoryPage: (query) => request('/category/page', { query }),
  categoryList: (type) => request('/category/list', { query: { type } }),
  categorySave: (body) => request('/category', { method: 'POST', body }),
  categoryUpdate: (body) => request('/category', { method: 'PUT', body }),
  categoryDelete: (id) => request('/category', { method: 'DELETE', query: { id } }),
  categoryStatus: (status, id) => request(`/category/status/${status}`, { method: 'POST', query: { id } }),

  /* ---- 菜品 ---- */
  dishPage: (query) => request('/dish/page', { query }),
  dishGet: (id) => request(`/dish/${id}`),
  dishList: (categoryId) => request('/dish/list', { query: { categoryId } }),
  dishSave: (body) => request('/dish', { method: 'POST', body }),
  dishUpdate: (body) => request('/dish', { method: 'PUT', body }),
  dishDelete: (ids) => request('/dish', { method: 'DELETE', query: { ids } }),
  dishStatus: (status, id) => request(`/dish/status/${status}`, { method: 'POST', query: { id } }),
  dishStock: (dishId) => request(`/dish/stock/${dishId}`),
  dishStockSet: (body) => request('/dish/stock', { method: 'PUT', body }),

  /* ---- 套餐 ---- */
  setmealPage: (query) => request('/setmeal/page', { query }),
  setmealGet: (id) => request(`/setmeal/${id}`),
  setmealSave: (body) => request('/setmeal', { method: 'POST', body }),
  setmealUpdate: (body) => request('/setmeal', { method: 'PUT', body }),
  setmealDelete: (ids) => request('/setmeal', { method: 'DELETE', query: { ids } }),
  setmealStatus: (status, id) => request(`/setmeal/status/${status}`, { method: 'POST', query: { id } }),

  /* ---- 订单 ---- */
  orderPage: (query) => request('/order/conditionSearch', { query }),
  orderDetail: (id) => request(`/order/details/${id}`),
  orderConfirm: (id) => request('/order/confirm', { method: 'PUT', body: { id } }),
  orderComplete: (id) => request(`/order/complete/${id}`, { method: 'PUT' }),
  orderPayStatus: (id, payStatus) => request('/order/payStatus', { method: 'PUT', body: { id, payStatus } }),
  orderRejection: (body) => request('/order/rejection', { method: 'PUT', body }),
  orderCancel: (body) => request('/order/cancel', { method: 'PUT', body }),
  orderStatistics: () => request('/order/statistics'),

  /* ---- 店铺 ---- */
  shopStatus: () => request('/shop/status'),
  shopSet: (status) => request(`/shop/${status}`, { method: 'PUT' }),
  shopAutoAccept: () => request('/shop/autoAccept'),
  shopAutoAcceptSet: (body) => request('/shop/autoAccept', { method: 'PUT', body }),

  /* ---- 工作台 ---- */
  businessData: () => request('/workspace/businessData'),
  overviewOrders: () => request('/workspace/overviewOrders'),
  overviewDishes: () => request('/workspace/overviewDishes'),
  overviewSetmeals: () => request('/workspace/overviewSetmeals'),

  /* ---- 报表 ---- */
  reportTurnover: (begin, end) => request('/report/turnoverStatistics', { query: { begin, end } }),
  reportUsers: (begin, end) => request('/report/userStatistics', { query: { begin, end } }),
  reportOrders: (begin, end) => request('/report/ordersStatistics', { query: { begin, end } }),
  reportTop10: (begin, end) => request('/report/top10', { query: { begin, end } }),
  reportExport: () => api.download('/report/export', 'business-data.xlsx'),

  /* ---- 堂食 ---- */
  diningTables: (query) => request('/dining/table/list', { query }),
  diningTableCreate: (body) => request('/dining/table', { method: 'POST', body }),
  diningTableUpdate: (id, body) => request(`/dining/table/${id}`, { method: 'PUT', body }),
  diningTableDelete: (id) => request(`/dining/table/${id}`, { method: 'DELETE' }),
  diningTableRefreshQr: (id) => request(`/dining/table/${id}/refresh-qr`, { method: 'POST' }),
  diningReservations: (query) => request('/dining/reservation/list', { query }),
  diningReservationConfirm: (id) => request(`/dining/reservation/${id}/confirm`, { method: 'POST' }),
  diningReservationCancel: (id) => request(`/dining/reservation/${id}/cancel`, { method: 'POST' }),
  diningReservationArrive: (id) => request(`/dining/reservation/${id}/arrive`, { method: 'POST' }),
  diningSessions: (query) => request('/dining/session/list', { query }),
  diningSession: (id) => request(`/dining/session/${id}`),
  diningOpen: (body) => request('/dining/session/open', { method: 'POST', body }),
  diningTransfer: (id, body) => request(`/dining/session/${id}/transfer`, { method: 'POST', body }),
  diningMerge: (targetId, body) => request(`/dining/session/${targetId}/merge`, { method: 'POST', body }),
  diningCancel: (id, reason) => request(`/dining/session/${id}/cancel`, { method: 'POST', query: { reason } }),
  diningClose: (id, reason) => request(`/dining/session/${id}/close`, { method: 'POST', query: { reason } }),
  diningCheckout: (body) => request('/dining/session/checkout', { method: 'POST', body }),
  diningReport: (query) => request('/dining/report', { query }),

  /* ---- 通用 ---- */
  upload: (file) => {
    const form = new FormData();
    form.append('file', file, file.name);
    return request('/common/upload', { method: 'POST', form });
  }
};
