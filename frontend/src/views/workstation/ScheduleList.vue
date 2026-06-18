<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">排班与开工管理</span>
      <el-button type="primary" @click="handleAddSchedule">
        <el-icon><Plus /></el-icon>
        新增排班
      </el-button>
    </div>

    <div class="search-bar">
      <el-date-picker v-model="searchForm.scheduleDate" type="date" value-format="YYYY-MM-DD" placeholder="排班日期" style="width: 180px" />
      <el-select v-model="searchForm.workstationId" placeholder="选择工位" clearable filterable style="width: 180px">
        <el-option v-for="ws in stationList" :key="ws.id" :label="`${ws.stationNo} ${ws.stationName}`" :value="ws.id" />
      </el-select>
      <el-select v-model="searchForm.welderId" placeholder="选择焊工" clearable filterable style="width: 180px">
        <el-option v-for="w in welderList" :key="w.id" :label="`${w.welderNo} ${w.welderName}`" :value="w.id" />
      </el-select>
      <el-select v-model="searchForm.scheduleStatus" placeholder="排班状态" clearable style="width: 140px">
        <el-option label="已排班" value="SCHEDULED" />
        <el-option label="开工中" value="WORKING" />
        <el-option label="首检中" value="INSPECTING" />
        <el-option label="已暂停" value="SUSPENDED" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="scheduleNo" label="排班单号" width="180" />
      <el-table-column label="工位" width="140">
        <template #default="{ row }">
          {{ getStationName(row.workstationId) }}
        </template>
      </el-table-column>
      <el-table-column label="焊工" width="120">
        <template #default="{ row }">
          {{ getWelderName(row.welderId) }}
        </template>
      </el-table-column>
      <el-table-column label="工艺卡" width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ getCardName(row.processCardId) }}
        </template>
      </el-table-column>
      <el-table-column prop="scheduleDate" label="排班日期" width="120" />
      <el-table-column prop="shiftType" label="班次" width="80">
        <template #default="{ row }">
          {{ row.shiftType === 'NIGHT' ? '夜班' : '白班' }}
        </template>
      </el-table-column>
      <el-table-column prop="taskDesc" label="任务描述" show-overflow-tooltip />
      <el-table-column label="排班状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.scheduleStatus)" effect="dark" size="small">
            {{ statusText(row.scheduleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="actualStartTime" label="实际开始" width="170" />
      <el-table-column prop="actualEndTime" label="实际结束" width="170" />
      <el-table-column label="操作" width="380" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" link @click="checkPermit(row)">
              <el-icon><Key /></el-icon>
              开工检查
            </el-button>
            <el-button v-if="row.scheduleStatus === 'SCHEDULED'" size="small" type="warning" link @click="handleConfirmProcess(row)">
              <el-icon><DocumentChecked /></el-icon>
              确认工艺卡
            </el-button>
            <el-button v-if="row.scheduleStatus === 'SCHEDULED' || row.scheduleStatus === 'SUSPENDED'" size="small" type="success" link @click="handleStart(row)">
              <el-icon><VideoPlay /></el-icon>
              开工
            </el-button>
            <el-button v-if="row.scheduleStatus === 'WORKING'" size="small" type="warning" link @click="handleSubmitInspection(row)">
              <el-icon><CircleCheck /></el-icon>
              提交首检
            </el-button>
            <el-button v-if="row.scheduleStatus === 'WORKING' || row.scheduleStatus === 'INSPECTING'" size="small" type="success" link @click="handleComplete(row)">
              <el-icon><Check /></el-icon>
              完工
            </el-button>
            <el-button v-if="row.scheduleStatus === 'WORKING' || row.scheduleStatus === 'INSPECTING'" size="small" type="danger" link @click="handleSuspend(row)">
              <el-icon><VideoPause /></el-icon>
              暂停
            </el-button>
            <el-button v-if="row.scheduleStatus === 'SUSPENDED'" size="small" type="primary" link @click="handleResume(row)">
              <el-icon><RefreshRight /></el-icon>
              恢复
            </el-button>
            <el-button v-if="row.scheduleStatus === 'SCHEDULED'" size="small" type="info" link @click="handleCancel(row)">取消</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 16px"
      background
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      :current-page="page.pageNum"
      :page-size="page.pageSize"
      :page-sizes="[10, 20, 50]"
      @current-change="p => (page.pageNum = p, loadData())"
      @size-change="s => (page.pageSize = s, page.pageNum = 1, loadData())"
    />

    <el-dialog v-model="scheduleDialog" title="新增排班" width="640px" destroy-on-close>
      <el-form :model="scheduleForm" label-width="110px" ref="scheduleFormRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工位" prop="workstationId" :rules="{ required: true, message: '请选择工位' }">
              <el-select v-model="scheduleForm.workstationId" filterable placeholder="请选择空闲工位" style="width: 100%">
                <el-option
                  v-for="ws in idleStationList"
                  :key="ws.id"
                  :label="`${ws.stationNo} ${ws.stationName}（${ws.area}）`"
                  :value="ws.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="焊工" prop="welderId" :rules="{ required: true, message: '请选择焊工' }">
              <el-select v-model="scheduleForm.welderId" filterable placeholder="请选择焊工" style="width: 100%">
                <el-option
                  v-for="w in welderList"
                  :key="w.id"
                  :label="`${w.welderNo} ${w.welderName}`"
                  :value="w.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工艺卡" prop="processCardId" :rules="{ required: true, message: '请选择工艺卡' }">
              <el-select v-model="scheduleForm.processCardId" filterable placeholder="请选择已下发工艺卡" style="width: 100%">
                <el-option
                  v-for="c in issuedCardList"
                  :key="c.id"
                  :label="`${c.cardNo} ${c.cardName}`"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排班日期" prop="scheduleDate" :rules="{ required: true, message: '请选择排班日期' }">
              <el-date-picker v-model="scheduleForm.scheduleDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次">
              <el-select v-model="scheduleForm.shiftType" style="width: 100%">
                <el-option label="白班" value="DAY" />
                <el-option label="夜班" value="NIGHT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划时间">
              <el-time-picker
                v-model="scheduleForm.timeRange"
                is-range
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="HH:mm"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="任务描述">
          <el-input v-model="scheduleForm.taskDesc" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialog = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">确认排班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permitDialog" title="开工许可检查" width="560px" destroy-on-close>
      <div v-if="permitStatus" style="padding: 10px 0;">
        <el-result
          :icon="permitStatus.canStart ? 'success' : 'error'"
          :title="permitStatus.canStart ? '满足开工条件' : '不满足开工条件'"
          :sub-title="permitStatus.message"
        />
        <el-descriptions :column="1" border style="margin-top: 16px;">
          <el-descriptions-item label="焊工资格">
            <el-tag :type="permitStatus.certValid ? 'success' : 'danger'" effect="dark">
              {{ permitStatus.certValid ? '有效' : '已过期' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="工艺卡确认">
            <el-tag :type="permitStatus.processConfirmed ? 'success' : 'warning'" effect="dark">
              {{ permitStatus.confirmStatus === 'CONFIRMED' ? '已确认' : (permitStatus.confirmStatus === 'REJECTED' ? '已拒绝' : '待确认') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="安全检查">
            <el-tag :type="permitStatus.safetyPassed ? 'success' : 'danger'" effect="dark">
              {{ permitStatus.safetyPassed ? '通过' : '未通过' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px; display: flex; gap: 10px;">
          <el-button type="primary" @click="openSafetyCheck">
            <el-icon><Safety /></el-icon>
            安全检查
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="confirmDialog" title="焊工确认工艺卡" width="600px" destroy-on-close>
      <el-descriptions v-if="currentCard" :column="1" border title="工艺卡信息">
        <el-descriptions-item label="工艺卡编号">{{ currentCard.cardNo }}</el-descriptions-item>
        <el-descriptions-item label="工艺卡名称">{{ currentCard.cardName }}</el-descriptions-item>
        <el-descriptions-item label="项目/分段">{{ currentCard.projectName }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ currentCard.workSection }}</el-descriptions-item>
        <el-descriptions-item label="母材">{{ currentCard.material }} {{ currentCard.materialSpec }}</el-descriptions-item>
        <el-descriptions-item label="焊接方法">{{ currentCard.weldMethod }}</el-descriptions-item>
        <el-descriptions-item label="焊接材料">{{ currentCard.weldMaterial }}</el-descriptions-item>
        <el-descriptions-item label="电流/电压">{{ currentCard.weldCurrent }} / {{ currentCard.weldVoltage }}</el-descriptions-item>
        <el-descriptions-item label="预热温度">{{ currentCard.preheatTemp || '无' }}</el-descriptions-item>
        <el-descriptions-item label="层间温度">{{ currentCard.interpassTemp || '无' }}</el-descriptions-item>
        <el-descriptions-item label="技术要求">{{ currentCard.techRequirement }}</el-descriptions-item>
        <el-descriptions-item label="质量标准">{{ currentCard.qualityStandard }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 20px;">
        <el-radio-group v-model="confirmResult">
          <el-radio :value="'CONFIRMED'">确认无误，同意执行</el-radio>
          <el-radio :value="'REJECTED'">有异议，拒绝确认</el-radio>
        </el-radio-group>
        <el-input
          v-if="confirmResult === 'REJECTED'"
          v-model="rejectReason"
          type="textarea"
          :rows="3"
          placeholder="请输入拒绝原因"
          style="margin-top: 12px;"
        />
      </div>
      <template #footer>
        <el-button @click="confirmDialog = false">取消</el-button>
        <el-button type="primary" @click="submitConfirm">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="safetyDialog" title="安全检查" width="600px" destroy-on-close>
      <div style="margin-bottom: 12px;">
        <el-button size="small" @click="addSafetyItem">
          <el-icon><Plus /></el-icon>
          添加检查项
        </el-button>
      </div>
      <el-table :data="safetyCheckList" border size="small">
        <el-table-column label="检查项目" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.checkItem" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="检查结果" width="140">
          <template #default="{ row }">
            <el-radio-group v-model="row.checkResult">
              <el-radio :value="1">合格</el-radio>
              <el-radio :value="0">不合格</el-radio>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.checkDesc" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button type="danger" link size="small" @click="safetyCheckList.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="safetyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitSafetyCheck">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="suspendDialog" title="暂停原因" width="480px" destroy-on-close>
      <el-input v-model="suspendReason" type="textarea" :rows="4" placeholder="请输入暂停原因" />
      <template #footer>
        <el-button @click="suspendDialog = false">取消</el-button>
        <el-button type="primary" @click="doSuspend">确认暂停</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSchedulePage, createSchedule, startWork, confirmProcessCard, submitFirstInspection,
  completeWork, suspendWork, resumeWork, cancelSchedule,
  getStartPermitStatus, getSafetyChecks, saveSafetyChecks, getWorkstationList
} from '@/api/workstation'
import { getWelderList } from '@/api/welder'
import { getIssuedProcessCardList, getProcessCard } from '@/api/processCard'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const searchForm = reactive({ scheduleDate: '', workstationId: null, welderId: null, scheduleStatus: '' })

const stationList = ref([])
const welderList = ref([])
const issuedCardList = ref([])

const idleStationList = computed(() => stationList.value.filter(s => s.currentStatus === 'IDLE' && s.status === 1))

const statusText = (s) => ({
  SCHEDULED: '已排班', STARTED: '已开工', WORKING: '开工中',
  INSPECTING: '首检中', COMPLETED: '已完成', SUSPENDED: '已暂停', CANCELLED: '已取消'
})[s] || s

const statusType = (s) => ({
  SCHEDULED: '', STARTED: '', WORKING: 'success',
  INSPECTING: 'warning', COMPLETED: 'info', SUSPENDED: 'danger', CANCELLED: 'info'
})[s] || ''

const getStationName = (id) => stationList.value.find(s => s.id === id)?.stationName || ''
const getWelderName = (id) => welderList.value.find(w => w.id === id)?.welderName || ''
const getCardName = (id) => issuedCardList.value.find(c => c.id === id)?.cardName || ''

const scheduleDialog = ref(false)
const scheduleFormRef = ref()
const scheduleForm = reactive({
  workstationId: null, welderId: null, processCardId: null, scheduleDate: '',
  shiftType: 'DAY', timeRange: null, taskDesc: ''
})

const permitDialog = ref(false)
const permitStatus = ref(null)
const currentSchedule = ref(null)

const confirmDialog = ref(false)
const confirmResult = ref('CONFIRMED')
const rejectReason = ref('')
const currentCard = ref(null)

const safetyDialog = ref(false)
const safetyCheckList = ref([])

const suspendDialog = ref(false)
const suspendReason = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSchedulePage({ ...searchForm, pageNum: page.pageNum, pageSize: page.pageSize })
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const loadBaseData = async () => {
  try { stationList.value = await getWorkstationList() || [] } catch (e) {}
  try { welderList.value = await getWelderList() || [] } catch (e) {}
  try { issuedCardList.value = await getIssuedProcessCardList() || [] } catch (e) {}
}

const resetSearch = () => {
  Object.assign(searchForm, { scheduleDate: '', workstationId: null, welderId: null, scheduleStatus: '' })
  loadData()
}

const handleAddSchedule = () => {
  Object.assign(scheduleForm, { workstationId: null, welderId: null, processCardId: null, scheduleDate: '', shiftType: 'DAY', timeRange: null, taskDesc: '' })
  scheduleDialog.value = true
}

const submitSchedule = async () => {
  scheduleFormRef.value.validate(async (valid) => {
    if (!valid) return
    const data = { ...scheduleForm }
    if (data.timeRange && data.timeRange.length === 2 && data.scheduleDate) {
      data.planStartTime = `${data.scheduleDate} ${data.timeRange[0]}:00`
      data.planEndTime = `${data.scheduleDate} ${data.timeRange[1]}:00`
    }
    delete data.timeRange
    await createSchedule(data, 2)
    ElMessage.success('排班成功')
    scheduleDialog.value = false
    loadData()
    loadBaseData()
  })
}

const checkPermit = async (row) => {
  currentSchedule.value = row
  permitStatus.value = await getStartPermitStatus(row.id)
  permitDialog.value = true
}

const handleConfirmProcess = async (row) => {
  currentSchedule.value = row
  currentCard.value = await getProcessCard(row.processCardId)
  confirmResult.value = 'CONFIRMED'
  rejectReason.value = ''
  confirmDialog.value = true
}

const submitConfirm = async () => {
  await confirmProcessCard({
    scheduleId: currentSchedule.value.id,
    welderId: currentSchedule.value.welderId,
    processCardId: currentSchedule.value.processCardId,
    confirmStatus: confirmResult.value,
    rejectReason: confirmResult.value === 'REJECTED' ? rejectReason.value : ''
  })
  ElMessage.success(confirmResult.value === 'CONFIRMED' ? '确认成功' : '已提交拒绝')
  confirmDialog.value = false
  loadData()
}

const openSafetyCheck = async () => {
  safetyCheckList.value = await getSafetyChecks(currentSchedule.value.id) || []
  if (safetyCheckList.value.length === 0) {
    safetyCheckList.value = [
      { scheduleId: currentSchedule.value.id, workstationId: currentSchedule.value.workstationId, checkType: 'PREWORK', checkItem: '设备运行正常', checkResult: null, checkDesc: '' },
      { scheduleId: currentSchedule.value.id, workstationId: currentSchedule.value.workstationId, checkType: 'PREWORK', checkItem: '防护用品佩戴齐全', checkResult: null, checkDesc: '' },
      { scheduleId: currentSchedule.value.id, workstationId: currentSchedule.value.workstationId, checkType: 'PREWORK', checkItem: '作业区域安全无隐患', checkResult: null, checkDesc: '' },
      { scheduleId: currentSchedule.value.id, workstationId: currentSchedule.value.workstationId, checkType: 'PREWORK', checkItem: '消防器材到位有效', checkResult: null, checkDesc: '' }
    ]
  }
  safetyDialog.value = true
  permitDialog.value = false
}

const addSafetyItem = () => {
  safetyCheckList.value.push({
    scheduleId: currentSchedule.value.id,
    workstationId: currentSchedule.value.workstationId,
    checkType: 'PREWORK', checkItem: '', checkResult: null, checkDesc: ''
  })
}

const submitSafetyCheck = async () => {
  await saveSafetyChecks(safetyCheckList.value)
  ElMessage.success('保存成功')
  safetyDialog.value = false
  permitStatus.value = await getStartPermitStatus(currentSchedule.value.id)
  permitDialog.value = true
}

const handleStart = (row) => {
  ElMessageBox.confirm('确定要开工吗？开工前将自动检查焊工资格、工艺卡确认和安全检查。', '提示', { type: 'warning' }).then(async () => {
    await startWork(row.id, row.welderId)
    ElMessage.success('开工成功')
    loadData()
    loadBaseData()
  })
}

const handleSubmitInspection = (row) => {
  ElMessageBox.confirm('确定要提交首件检查吗？提交后将进入首检状态，等待质检员检查。', '提示', { type: 'warning' }).then(async () => {
    await submitFirstInspection(row.id)
    ElMessage.success('已提交首检申请')
    loadData()
    loadBaseData()
  })
}

const handleComplete = (row) => {
  ElMessageBox.confirm('确定要标记完工吗？', '提示', { type: 'warning' }).then(async () => {
    await completeWork(row.id)
    ElMessage.success('已标记完工')
    loadData()
    loadBaseData()
  })
}

const handleSuspend = (row) => {
  currentSchedule.value = row
  suspendReason.value = ''
  suspendDialog.value = true
}

const doSuspend = async () => {
  if (!suspendReason.value.trim()) {
    ElMessage.warning('请输入暂停原因')
    return
  }
  await suspendWork(currentSchedule.value.id, suspendReason.value)
  ElMessage.success('已暂停')
  suspendDialog.value = false
  loadData()
  loadBaseData()
}

const handleResume = (row) => {
  ElMessageBox.confirm('确定要恢复作业吗？', '提示', { type: 'warning' }).then(async () => {
    await resumeWork(row.id)
    ElMessage.success('已恢复作业')
    loadData()
    loadBaseData()
  })
}

const handleCancel = (row) => {
  ElMessageBox.confirm('确定要取消该排班吗？', '提示', { type: 'warning' }).then(async () => {
    await cancelSchedule(row.id)
    ElMessage.success('已取消')
    loadData()
    loadBaseData()
  })
}

onMounted(async () => {
  await loadBaseData()
  loadData()
})
</script>
