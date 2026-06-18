<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">工序暂停记录</span>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="suspensionNo" label="暂停单号" width="180" />
      <el-table-column label="工位" width="140">
        <template #default="{ row }">
          {{ getStationName(row.workstationId) }}
        </template>
      </el-table-column>
      <el-table-column prop="suspensionType" label="暂停类型" width="160">
        <template #default="{ row }">
          {{ typeText(row.suspensionType) }}
        </template>
      </el-table-column>
      <el-table-column prop="suspensionReason" label="暂停原因" show-overflow-tooltip />
      <el-table-column label="暂停状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.suspensionStatus === 'ACTIVE' ? 'danger' : 'success'" effect="dark" size="small">
            {{ row.suspensionStatus === 'ACTIVE' ? '生效中' : '已解除' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否影响后续工序" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.affectNextProcess === 1" type="danger" size="small">是</el-tag>
          <el-tag v-else type="info" size="small">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reportTime" label="上报时间" width="170" />
      <el-table-column prop="resolveTime" label="解除时间" width="170" />
      <el-table-column prop="resolveMeasure" label="解除措施" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.suspensionStatus === 'ACTIVE'"
            size="small"
            type="success"
            link
            @click="handleResolve(row)"
          >
            解除暂停
          </el-button>
          <el-button size="small" type="primary" link @click="handleView(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="resolveDialog" title="解除暂停" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="解除措施">
          <el-input v-model="resolveMeasure" type="textarea" :rows="4" placeholder="请输入解除措施" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialog = false">取消</el-button>
        <el-button type="primary" @click="submitResolve">确认解除</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialog" title="暂停详情" width="640px" destroy-on-close>
      <el-descriptions :column="1" border v-if="currentRow">
        <el-descriptions-item label="暂停单号">{{ currentRow.suspensionNo }}</el-descriptions-item>
        <el-descriptions-item label="工位">{{ getStationName(currentRow.workstationId) }}</el-descriptions-item>
        <el-descriptions-item label="暂停类型">{{ typeText(currentRow.suspensionType) }}</el-descriptions-item>
        <el-descriptions-item label="暂停状态">
          <el-tag :type="currentRow.suspensionStatus === 'ACTIVE' ? 'danger' : 'success'" effect="dark">
            {{ currentRow.suspensionStatus === 'ACTIVE' ? '生效中' : '已解除' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="影响后续工序">
          {{ currentRow.affectNextProcess === 1 ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="暂停原因">{{ currentRow.suspensionReason }}</el-descriptions-item>
        <el-descriptions-item label="上报时间">{{ currentRow.reportTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.resolveTime" label="解除时间">{{ currentRow.resolveTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentRow.resolveMeasure" label="解除措施">{{ currentRow.resolveMeasure }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSuspensionList, resolveSuspension } from '@/api/inspection'
import { getWorkstationList } from '@/api/workstation'

const loading = ref(false)
const tableData = ref([])
const stationList = ref([])

const resolveDialog = ref(false)
const viewDialog = ref(false)
const currentRow = ref(null)
const resolveRow = ref(null)
const resolveMeasure = ref('')

const typeText = (t) => ({
  FIRST_ARTICLE_FAIL: '首件不合格',
  SAFETY_ISSUE: '安全问题',
  CERT_EXPIRED: '资格过期',
  OTHER: '其他'
})[t] || t

const getStationName = (id) => stationList.value.find(s => s.id === id)?.stationName || ''

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await getSuspensionList() || []
  } finally {
    loading.value = false
  }
}

const handleResolve = (row) => {
  resolveRow.value = row
  resolveMeasure.value = ''
  resolveDialog.value = true
}

const submitResolve = async () => {
  if (!resolveMeasure.value.trim()) {
    ElMessage.warning('请输入解除措施')
    return
  }
  await resolveSuspension(resolveRow.value.id, 5, resolveMeasure.value)
  ElMessage.success('解除成功')
  resolveDialog.value = false
  loadData()
}

const handleView = (row) => {
  currentRow.value = row
  viewDialog.value = true
}

onMounted(async () => {
  try { stationList.value = await getWorkstationList() || [] } catch (e) {}
  loadData()
})
</script>
