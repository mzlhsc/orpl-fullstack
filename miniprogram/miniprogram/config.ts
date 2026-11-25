/**
 * 本地开发默认直连 Spring Boot 的 8080 端口。
 * 如果通过 nginx 80 端口代理，可改为 http://127.0.0.1。
 * 正式发布时请改为已备案、已配置 request 合法域名的 HTTPS 地址。
 */
export const API_BASE = 'http://121.40.186.5'

/**
 * 后端未配置微信 appid/secret 时，会把登录 code 直接当作 openid。
 * 本地联调固定使用同一个 code，避免每次 wx.login 产生新用户。
 * 配置真实微信密钥后，将它改为 false，走真实 wx.login + 微信登录。
 */
export const USE_MOCK_LOGIN = true
export const MOCK_LOGIN_CODE = 'ordpf-mp-local-user'

export const STORAGE_KEYS = {
  token: 'op_token',
  user: 'op_user',
  selectedAddress: 'op_selected_address',
  localNickname: 'op_local_nickname',
} as const

export const FOOD_PLACEHOLDER = '/assets/food/placeholder.jpg'

export const SHOP_PHONE = '400-000-0000'
