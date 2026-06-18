import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '工作台' }
  },
  {
    path: '/welder',
    name: 'Welder',
    component: () => import('@/views/welder/WelderList.vue'),
    meta: { title: '焊工资格管理' }
  },
  {
    path: '/process-card',
    name: 'ProcessCard',
    component: () => import('@/views/process/ProcessCardList.vue'),
    meta: { title: '工艺卡管理' }
  },
  {
    path: '/workstation',
    name: 'Workstation',
    component: () => import('@/views/workstation/WorkstationList.vue'),
    meta: { title: '工位管理' }
  },
  {
    path: '/schedule',
    name: 'Schedule',
    component: () => import('@/views/workstation/ScheduleList.vue'),
    meta: { title: '排班与开工' }
  },
  {
    path: '/inspection',
    name: 'Inspection',
    component: () => import('@/views/inspection/InspectionList.vue'),
    meta: { title: '首件质检' }
  },
  {
    path: '/suspension',
    name: 'Suspension',
    component: () => import('@/views/inspection/SuspensionList.vue'),
    meta: { title: '工序暂停记录' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 船厂焊接工位开工许可系统` : '船厂焊接工位开工许可系统'
  next()
})

export default router
