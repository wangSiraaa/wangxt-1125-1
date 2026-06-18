<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <el-icon :size="28" color="#fff"><Tools /></el-icon>
        <span class="title">船厂焊接工位开工许可系统</span>
      </div>
      <div class="header-right">
        <el-dropdown>
          <span class="user-info">
            <el-avatar :size="32" style="background: #409EFF">
              {{ userInfo.realName?.charAt(0) || '管' }}
            </el-avatar>
            <span class="username">{{ userInfo.realName || '管理员' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item>个人设置</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#001529"
          text-color="#fff"
          active-text-color="#ffd04b"
        >
          <el-menu-item index="/dashboard">
            <el-icon><HomeFilled /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/welder">
            <el-icon><User /></el-icon>
            <span>焊工资格管理</span>
          </el-menu-item>
          <el-menu-item index="/process-card">
            <el-icon><Document /></el-icon>
            <span>工艺卡管理</span>
          </el-menu-item>
          <el-menu-item index="/workstation">
            <el-icon><Location /></el-icon>
            <span>工位管理</span>
          </el-menu-item>
          <el-menu-item index="/schedule">
            <el-icon><Calendar /></el-icon>
            <span>排班与开工</span>
          </el-menu-item>
          <el-menu-item index="/inspection">
            <el-icon><CircleCheck /></el-icon>
            <span>首件质检</span>
          </el-menu-item>
          <el-menu-item index="/suspension">
            <el-icon><Warning /></el-icon>
            <span>工序暂停记录</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)

const userInfo = ref({
  realName: '系统管理员',
  roleCode: 'ADMIN'
})

const logout = () => {
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.layout-header {
  background: linear-gradient(90deg, #1d4ed8 0%, #001529 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}
.header-right .user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  cursor: pointer;
}
.username {
  font-size: 14px;
}
.layout-aside {
  background: #001529;
}
:deep(.el-menu) {
  border-right: none;
}
.layout-main {
  background: #f0f2f5;
  padding: 20px;
  overflow: auto;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
