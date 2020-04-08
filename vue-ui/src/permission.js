import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

/**
 * 在这里写下首次访问的逻辑：
 *  1.访问localhost:80 什么都不带,首先根据默认的路由配置重定向到index，接下里走的就是路由拦截器到这里。
 *  2.先判断你是否有token,第一次登录肯定没有，接下来看你访问的地址是否在白名单whiteList，也没有
 *  3. 然后就重定向到login页面了。next(`/login?redirect=${to.path}`) 也就是地址栏的http://localhost/login?redirect=%2Findex
 * ===============
 *  默认在login页面登录完后后跳转到index
 */

const whiteList = ['/login', '/auth-redirect', '/bind', '/register']

router.beforeEach((to, from, next) => {
  NProgress.start()
  // 登录成功后token已经set过了
  if (getToken()) {
    /* has token*/
    // 如果你的访问路径是login,还让你访问'/，也就是再redirect到index。已经有token了不用再登录
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      // 如果你访问的是其他页面,当时这里不光光是首次登录，有token的情况下每次访问都会走这里，判断你的权限。
      // 如果有权限直接放行,如果roles<>0 继续往下走获取权限
      if (store.getters.roles.length === 0) {
        // 判断当前用户是否已拉取完user_info信息
        store.dispatch('GetInfo').then(res => {
          // 拉取user_info
          const roles = res.roles
          store.dispatch('GenerateRoutes', { roles }).then(accessRoutes => {
          // 测试 默认静态页面
          // store.dispatch('permission/generateRoutes', { roles }).then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            router.addRoutes(accessRoutes) // 动态添加可访问路由表
            next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
          })
        })
          .catch(err => {
            store.dispatch('FedLogOut').then(() => {
              Message.error(err)
              next({ path: '/' })
            })
          })
      } else {
        next()
        // 没有动态改变权限的需求可直接next() 删除下方权限判断 ↓
        // if (hasPermission(store.getters.roles, to.meta.roles)) {
        //   next()
        // } else {
        //   next({ path: '/401', replace: true, query: { noGoBack: true }})
        // }
        // 可删 ↑
      }
    }
  } else {
    // 没有token,看是否在登录白名单,白名单是不需要登录就能访问的 页面
    if (whiteList.indexOf(to.path) !== -1) {
      // 在免登录白名单，直接进入
      next()
    } else {
      // 否则全部重定向到登录页
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
