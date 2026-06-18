<template>
  <div class="page-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
          <el-icon :size="24"><User /></el-icon>
          <div class="stat-value">{{ stats.totalWelders }}</div>
          <div class="stat-label">焊工总数</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
          <el-icon :size="24"><Warning /></el-icon>
          <div class="stat-value">{{ stats.expiringCerts }}</div>
          <div class="stat-label">即将过期/已过期证书</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
          <el-icon :size="24"><Location /></el-icon>
          <div class="stat-value">{{ stats.workingStations }}</div>
          <div class="stat-label">开工中工位</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
          <el-icon :size="24"><CircleCheck /></el-icon>
          <div class="stat-value">{{ stats.todaySchedules }}</div>
          <div class="stat-label">今日排班</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>焊工证书状态预警</span></div>
          </template>
          <el-table :data="expiringCertList" size="small">
            <el-table-column prop="welderName" label="焊工" width="100" />
            <el-table-column prop="certNo" label="证书编号" width="150" />
            <el-table-column prop="certType" label="证书类型" width="180" />
            <el-table-column prop="expiryDate" label="到期日期" width="120" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.certStatus === 2 ? 'warning' : 'danger'" effect="dark" size="small">
                  {{ row.certStatus === 2 ? '即将过期' : '已过期' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>工位实时状态</span></div>
          </template>
          <el-table :data="stationStatusList" size="small">
            <el-table-column prop="stationNo" label="工位编号" width="100" />
            <el-table-column prop="stationName" label="工位名称" />
            <el-table-column prop="area" label="区域" width="120" />
            <el-table-column label="当前状态" width="120">
              <template #default="{ row }">
                <el-tag :type="stationStatusType(row.currentStatus)" effect="dark" size="small">
                  {{ stationStatusText(row.currentStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header"><span>今日排班情况</span></div>
          </template>
          <el-table :data="todayScheduleList" size="small">
            <el-table-column prop="scheduleNo" label="排班单号" width="160" />
            <el-table-column prop="stationName" label="工位" width="140" />
            <el-table-column prop="welderName" label="焊工" width="100" />
            <el-table-column prop="cardName" label="工艺卡" />
            <el-table-column prop="planStartTime" label="计划时间" width="180" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="scheduleStatusType(row.scheduleStatus)" effect="dark" size="small">
                  {{ scheduleStatusText(row.scheduleStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getWelderList, getCertPage, refreshCertStatus } from '@/api/welder'
import { getWorkstationList } from '@/api/workstation'
import { getSchedulePage } from '@/api/workstation'

const stats = ref({
  totalWelders: 0,
  expiringCerts: 0,
  workingStations: 0,
  todaySchedules: 0
})

const expiringCertList = ref([])
const stationStatusList = ref([])
const todayScheduleList = ref([])

const stationStatusText = (s) => ({
  IDLE: '空闲', SCHEDULED: '已排班', WORKING: '开工中',
  INSPECTING: '首检中', SUSPENDED: '已暂停'
})[s] || s

const stationStatusType = (s) => ({
  IDLE: 'info', SCHEDULED: '', WORKING: 'success',
  INSPECTING: 'warning', SUSPENDED: 'danger'
})[s] || ''

const scheduleStatusText = (s) => ({
  SCHEDULED: '已排班', STARTED: '已开工', WORKING: '开工中',
  INSPECTING: '首检中', COMPLETED: '已完成', SUSPENDED: '已暂停', CANCELLED: '已取消'
})[s] || s

const scheduleStatusType = (s) => ({
  SCHEDULED: '', STARTED: '', WORKING: 'success',
  INSPECTING: 'warning', COMPLETED: 'info', SUSPENDED: 'danger', CANCELLED: 'info'
})[s] || ''

const loadData = async () => {
  try {
    await refreshCertStatus()
  } catch (e) {}

  try {
    const welders = await getWelderList()
    stats.value.totalWelders = welders?.length || 0
  } catch (e) {}

  try {
    const certPage = await getCertPage({ pageNum: 1, pageSize: 5, certStatus: 2 }) || { records: [] }
    const certPage3 = await getCertPage({ pageNum: 1, pageSize: 5, certStatus: 3 }) || { records: [] }
    expiringCertList.value = [...(certPage.records || []), ...(certPage3.records || [])]
    stats.value.expiringCerts = (certPage.records?.length || 0) + (certPage3.records?.length || 0)
  } catch (e) {}

  try {
    const stations = await getWorkstationList() || []
    stationStatusList.value = stations
    stats.value.workingStations = stations.filter(s => s.currentStatus === 'WORKING' || s.currentStatus === 'INSPECTING').length
  } catch (e) {}

  try {
    const today = new Date().toISOString().split('T')[0]
    const schedPage = await getSchedulePage({ pageNum: 1, pageSize: 20, scheduleDate: today }) || { records: [] }
    todayScheduleList.value = schedPage.records || []
    stats.value.todaySchedules = todayScheduleList.value.length
  } catch (e) {}
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
</style>
