/* 店铺设置 */
import { api, auth } from '../api.js';
import { $, escapeHtml } from '../util.js';
import { openModal, setBtnLoading, statusTag, toast, toastError } from '../ui.js';

let shopOn = false;

function paintShop() {
  const sw = $('#setting-shop-switch');
  if (sw) sw.classList.toggle('on', shopOn);
  const tag = $('#setting-shop-tag');
  if (tag) {
    tag.className = `tag ${shopOn ? 'tag-success' : 'tag-danger'}`;
    tag.innerHTML = `<span class="tag-dot"></span>${shopOn ? '营业中' : '打烊中'}`;
  }
  const hint = $('#setting-shop-hint');
  if (hint) hint.textContent = shopOn ? '用户端可以正常浏览与下单。' : '用户端会提示门店休息，无法下单。';
}

async function toggleShop() {
  const sw = $('#setting-shop-switch');
  if (!sw || sw.disabled) return;
  sw.disabled = true;
  try {
    const next = shopOn ? 0 : 1;
    await api.shopSet(next);
    shopOn = next === 1;
    paintShop();
    window.dispatchEvent(new CustomEvent('op:shop-refresh'));
    toast(shopOn ? '店铺已开始营业' : '店铺已打烊', 'success');
  } catch (error) { toastError(error); } finally { sw.disabled = false; }
}

function openPasswordEdit() {
  const user = auth.user || {};
  openModal({
    title: '修改登录密码',
    subtitle: '当前登录账号：' + (user.userName || 'admin'),
    size: 'modal-sm',
    body: `<form id="pwd-form" class="form-grid" style="grid-template-columns:1fr" autocomplete="off">
      <div class="form-item"><label>当前密码<em>*</em></label><input class="input" type="password" name="oldPassword" autocomplete="current-password"></div>
      <div class="form-item"><label>新密码<em>*</em></label><input class="input" type="password" name="newPassword" autocomplete="new-password" placeholder="至少 6 位"></div>
      <div class="form-item"><label>确认新密码<em>*</em></label><input class="input" type="password" name="confirmPassword" autocomplete="new-password"></div>
      <p class="form-error" data-error hidden></p>
    </form>`,
    footer: `<button class="btn btn-ghost" type="button" data-close>取消</button><button class="btn btn-primary" type="button" data-save>保存</button>`,
    onMount: (modal, close) => {
      const form = modal.querySelector('#pwd-form');
      const errorEl = modal.querySelector('[data-error]');
      const saveBtn = modal.querySelector('[data-save]');
      saveBtn.addEventListener('click', async () => {
        errorEl.hidden = true;
        const oldPassword = form.oldPassword.value;
        const newPassword = form.newPassword.value;
        const confirmPassword = form.confirmPassword.value;
        if (!oldPassword || !newPassword) { errorEl.hidden = false; errorEl.textContent = '请填写当前密码和新密码'; return; }
        if (newPassword.length < 6) { errorEl.hidden = false; errorEl.textContent = '新密码至少 6 位'; return; }
        if (newPassword !== confirmPassword) { errorEl.hidden = false; errorEl.textContent = '两次输入的新密码不一致'; return; }
        if (oldPassword === newPassword) { errorEl.hidden = false; errorEl.textContent = '新密码不能与当前密码相同'; return; }
        setBtnLoading(saveBtn, true, '保存中…');
        try {
          await api.employeePassword({ empId: user.id, oldPassword, newPassword });
          toast('密码修改成功', 'success');
          close();
        } catch (error) {
          toastError(error);
          errorEl.hidden = false; errorEl.textContent = error.message;
        } finally { setBtnLoading(saveBtn, false); }
      });
    }
  });
}

export async function render(root) {
  const user = auth.user || {};
  root.innerHTML = `
    <div class="settings-grid">
      <div class="card">
        <div class="card-head"><div><h2>营业状态</h2><p>控制用户端能否下单</p></div><span id="setting-shop-tag">—</span></div>
        <div class="card-body card-body-pad">
          <div class="setting-row">
            <div class="setting-info"><strong>门店营业开关</strong><span id="setting-shop-hint">读取中…</span></div>
            <button class="switch brand" id="setting-shop-switch" type="button" aria-label="切换营业状态"></button>
          </div>
          <div class="setting-row">
            <div class="setting-info"><strong>状态存储</strong><span>营业状态保存在 Redis 的 <code>shop:status</code>，首次启动默认打烊。</span></div>
          </div>
          <div class="setting-row">
            <div class="setting-info"><strong>同步范围</strong><span>用户端 <code>GET /user/shop/status</code> 会立即读取最新状态。</span></div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div><h2>账号信息</h2><p>当前登录的员工账号</p></div></div>
        <div class="card-body card-body-pad">
          <div class="setting-row">
            <div class="setting-info"><strong>${escapeHtml(user.name || '管理员')}</strong><span>${escapeHtml(user.userName || '')}</span></div>
            <span class="avatar" style="width:38px;height:38px">${escapeHtml((user.name || '管').slice(0, 1))}</span>
          </div>
          <div class="setting-row">
            <div class="setting-info"><strong>员工 ID</strong><span>${escapeHtml(String(user.id ?? '—'))}</span></div>
          </div>
          <div class="setting-row">
            <div class="setting-info"><strong>登录密码</strong><span>如需修改，请点击右侧按钮。密码修改需要验证当前密码。</span></div>
            <button class="btn btn-ghost btn-sm" id="btn-edit-pwd" type="button">修改密码</button>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div><h2>接口与部署</h2><p>当前前端就是通过 nginx 代理访问后端</p></div></div>
        <div class="card-body card-body-pad">
          <div class="detail-list">
            <div class="detail-row"><span>前端入口</span><strong><code>/</code> → <code>html/admin</code></strong></div>
            <div class="detail-row"><span>接口前缀</span><strong><code>/api</code> → 后端 <code>/admin</code></strong></div>
            <div class="detail-row"><span>鉴权请求头</span><strong><code>zmlhsc</code></strong></div>
            <div class="detail-row"><span>后端服务</span><strong><code>application.yml</code> 中的 <code>server.port</code></strong></div>
            <div class="detail-row"><span>在线接口文档</span><strong><a href="/doc.html" target="_blank" rel="noopener" style="color:var(--brand-strong)">/doc.html</a></strong></div>
            <div class="detail-row"><span>健康检查</span><strong><a href="/health" target="_blank" rel="noopener" style="color:var(--brand-strong)">/health</a></strong></div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div><h2>使用提示</h2><p>管理端常用操作说明</p></div></div>
        <div class="card-body card-body-pad">
          <div class="detail-list">
            <div class="detail-row"><span>店铺打烊后</span><strong>用户端无法下单，但管理端仍可正常处理历史订单</strong></div>
            <div class="detail-row"><span>菜品库存</span><strong>库存为 0 时用户端不可下单，堂食加菜同样受限</strong></div>
            <div class="detail-row"><span>桌台二维码</span><strong>刷新后旧二维码立即失效，已开台会话不受影响</strong></div>
            <div class="detail-row"><span>报表导出</span><strong>导出最近 30 天运营数据，需要后端存在报表模板</strong></div>
          </div>
        </div>
      </div>
    </div>`;

  try {
    const status = await api.shopStatus();
    shopOn = Number(status) === 1;
    paintShop();
  } catch (error) {
    $('#setting-shop-hint').textContent = '店铺状态读取失败，请检查后端与 Redis。';
    toastError(error);
  }
  $('#setting-shop-switch').addEventListener('click', toggleShop);
  $('#btn-edit-pwd').addEventListener('click', openPasswordEdit);
}

export function destroy() {}
