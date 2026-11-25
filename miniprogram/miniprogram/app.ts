import { ensureLogin } from './utils/auth'

App<IAppOption>({
  globalData: {},
  onLaunch() {
    // 进入小程序即完成静默登录，页面只负责消费登录态。
    void ensureLogin().catch((error) => {
      console.warn('自动登录失败', error)
    })
  },
})
