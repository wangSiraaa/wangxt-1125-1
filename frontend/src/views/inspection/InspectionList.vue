<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">首件质检</span>
      <el-badge v-if="lockedCount > 0" :value="lockedCount" class="header-badge">
        <el-button type="danger" size="small" @click="showLockedList = !showLockedList">
          焊缝报工锁定
        </el-button>
      </el-badge>
    </div>

    <el-alert
      v-if="showLockedList && lockedInspections.length > 0"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 12px;"
    >
      <template #title>
        <span>有 {{ lockedInspections.length }} 条首检不合格记录已锁定焊缝报工入口，复检通过后自动解锁</span>
      </template>
      <div v-for="item in lockedInspections" :key="item.id" style="margin: 4px 0; font-size: 13px;">
        <el-tag type="danger" size="small" effect="dark">焊缝锁定</el-tag>
        <span style="margin-left: 6px;">{{ item.inspectionNo }} — 焊缝编号: {{ item._weldSeamNo || '-' }}</span>
        <el-button type="primary" size="small" link style="margin-left: 8px;" @click="handleView(item)">
          查看详情
        </el-button>
      </div>
    </el-alert>

    <div class="search-bar">
      <el-select v-model="searchForm.scheduleId" placeholder="选择排班单" clearable filterable style="width: 260px">
        <el-option v-for="s in inspectingList" :key="s.id" :label="s.scheduleNo" :value="s.id" />
      </el-select>
      <el-select v-model="searchForm.welderId" placeholder="选择焊工" clearable filterable style="width: 180px">
        <el-option v-for="w in welderList" :key="w.id" :label="`${w.welderNo} ${w.welderName}`" :value="w.id" />
      </el-select>
      <el-select v-model="searchForm.overallResult" placeholder="检查结果" clearable style="width: 140px">
        <el-option label="合格" :value="1" />
        <el-option label="不合格" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="inspectionNo" label="首检单号" width="180" />
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
      <el-table-column label="外观检查" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.appearanceCheck !== null" :type="row.appearanceCheck === 1 ? 'success' : 'danger'" size="small">
            {{ row.appearanceCheck === 1 ? '合格' : '不合格' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="尺寸检查" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.sizeCheck !== null" :type="row.sizeCheck === 1 ? 'success' : 'danger'" size="small">
            {{ row.sizeCheck === 1 ? '合格' : '不合格' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="无损检测" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.ndtCheck !== null" :type="row.ndtCheck === 1 ? 'success' : 'danger'" size="small">
            {{ row.ndtCheck === 1 ? '合格' : '不合格' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="综合判定" width="100">
        <template #default="{ row }">
          <el-tag :type="row.overallResult === 1 ? 'success' : 'danger'" effect="dark" size="small">
            {{ row.overallResult === 1 ? '合格' : '不合格' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="焊缝报工" width="110">
        <template #default="{ row }">
          <template v-if="row.overallResult === 0">
            <el-tag v-if="row._weldSeamLocked === true" type="danger" effect="dark" size="small">
              已锁定
            </el-tag>
            <el-tag v-else-if="row._weldSeamLocked === false" type="warning" size="small">
              未锁定
            </el-tag>
            <el-tag v-else type="info" size="small">无焊缝</el-tag>
          </template>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="inspectionTime" label="检查时间" width="170" />
      <el-table-column label="复检" width="160">
        <template #default="{ row }">
          <div v-if="row.recheckRequired === 1">
            <el-tag v-if="row.recheckResult !== null" :type="row.recheckResult === 1 ? 'success' : 'danger'" size="small">
              {{ row.recheckResult === 1 ? '复检合格' : '复检不合格' }}
            </el-tag>
            <el-button v-else type="warning" size="small" link @click="handleRecheck(row)">
              待复检
            </el-button>
          </div>
          <span v-else>无需复检</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="handleView(row)">查看</el-button>
          <el-button v-if="row.overallResult === 0 && row.recheckRequired === 1 && row.recheckResult === null" size="small" type="warning" link @click="handleRecheck(row)">复检</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isView ? '首件检查详情' : '提交首件检查'" width="780px" destroy-on-close>
      <el-form :model="form" label-width="110px" ref="formRef" :disabled="isView">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="外观检查" prop="appearanceCheck" :rules="{ required: true, message: '请选择' }">
              <el-radio-group v-model="form.appearanceCheck">
                <el-radio :value="1">合格</el-radio>
                <el-radio :value="0">不合格</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="尺寸检查" prop="sizeCheck" :rules="{ required: true, message: '请选择' }">
              <el-radio-group v-model="form.sizeCheck">
                <el-radio :value="1">合格</el-radio>
                <el-radio :value="0">不合格</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="无损检测">
              <el-radio-group v-model="form.ndtCheck">
                <el-radio :value="1">合格</el-radio>
                <el-radio :value="0">不合格</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="综合判定" prop="overallResult" :rules="{ required: true, message: '请选择' }">
              <el-radio-group v-model="form.overallResult">
                <el-radio :value="1">合格</el-radio>
                <el-radio :value="0">不合格</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="缺陷描述">
          <el-input v-model="form.defectDesc" type="textarea" :rows="3" placeholder="如不合格，请描述缺陷情况" />
        </el-form-item>
        <el-form-item label="整改意见">
          <el-input v-model="form.rectificationAdvice" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="是否需要复检">
          <el-switch v-model="form.recheckRequired" :active-value="1" :inactive-value="0" active-text="需要" inactive-text="不需要" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>

        <template v-if="isView && viewWeldSeamNo">
          <el-divider content-position="left">焊缝报工锁定状态</el-divider>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="焊缝编号">{{ viewWeldSeamNo }}</el-descriptions-item>
            <el-descriptions-item label="锁定状态">
              <el-tag v-if="viewWeldSeamLocked" type="danger" effect="dark" size="small">已锁定</el-tag>
              <el-tag v-else type="success" size="small">正常</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-table v-if="viewWeldSeamReports.length > 0" :data="viewWeldSeamReports" border size="small" style="margin-top: 8px; max-height: 200px; overflow-y: auto;">
            <el-table-column prop="reportNo" label="报工单号" width="160" />
            <el-table-column prop="weldStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.weldStatus === 'LOCKED'" type="danger" size="small">已锁定</el-tag>
                <el-tag v-else-if="row.weldStatus === 'IN_PROGRESS'" type="primary" size="small">施工中</el-tag>
                <el-tag v-else-if="row.weldStatus === 'COMPLETED'" type="success" size="small">已完成</el-tag>
                <el-tag v-else size="small">{{ row.weldStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lockReason" label="锁定原因" show-overflow-tooltip />
            <el-table-column prop="machineNo" label="机台编号" width="120" />
            <el-table-column prop="materialBatch" label="材料批号" width="120" />
          </el-table>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ isView ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!isView" type="primary" @click="submitForm">提交检查结果</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recheckDialog" title="复检" width="720px" destroy-on-close>
      <el-alert
        v-if="recheckDetail.weldSeamLocked"
        type="error"
        show-icon
        :closable="false"
        style="margin-bottom: 12px;"
        title="该首检不合格已触发焊缝报工锁定"
        :description="`焊缝编号 ${recheckDetail.weldSeamNo || '-'} 的所有报工入口已被锁定，复检通过后将自动解锁`"
      />
      <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="机台编号">{{ recheckDetail.machineNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="材料批号">{{ recheckDetail.materialBatch || '-' }}</el-descriptions-item>
        <el-descriptions-item label="返修原因" :span="2">{{ recheckDetail.repairReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="焊缝编号">{{ recheckDetail.weldSeamNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="焊缝报工状态">
          <el-tag v-if="recheckDetail.weldSeamLocked" type="danger" size="small">已锁定</el-tag>
          <el-tag v-else-if="recheckDetail.weldSeamNo" type="success" size="small">正常</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-table
        v-if="recheckDetail.weldSeamReports && recheckDetail.weldSeamReports.length > 0"
        :data="recheckDetail.weldSeamReports"
        border
        size="small"
        style="margin-bottom: 16px; max-height: 200px; overflow-y: auto;"
      >
        <el-table-column prop="reportNo" label="报工单号" width="160" />
        <el-table-column prop="weldStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.weldStatus === 'LOCKED'" type="danger" size="small">已锁定</el-tag>
            <el-tag v-else-if="row.weldStatus === 'IN_PROGRESS'" type="primary" size="small">施工中</el-tag>
            <el-tag v-else-if="row.weldStatus === 'COMPLETED'" type="success" size="small">已完成</el-tag>
            <el-tag v-else size="small">{{ row.weldStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lockReason" label="锁定原因" show-overflow-tooltip />
        <el-table-column prop="machineNo" label="机台编号" width="120" />
        <el-table-column prop="materialBatch" label="材料批号" width="120" />
      </el-table>

      <el-form label-width="80px">
        <el-form-item label="复检结果">
          <el-radio-group v-model="recheckResult">
            <el-radio :value="1">合格</el-radio>
            <el-radio :value="0">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recheckDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRecheck">确认</el-button>
      </template>
    </el-dialog>

    <div v-if="inspectingList.length > 0" style="margin-top: 20px;">
      <el-alert
        :title="`有 ${inspectingList.length} 个排班处于首检中状态，待质检员处理`"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 16px;"
      >
        <template #default>
          <div v-for="s in inspectingList" :key="s.id" style="margin: 6px 0;">
            <span>排班单号: {{ s.scheduleNo }} | 工位: {{ getStationName(s.workstationId) }} | 焊工: {{ getWelderName(s.welderId) }}</span>
            <el-button type="primary" size="small" style="margin-left: 12px;" @click="handleNewInspection(s)">去检查</el-button>
          </div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getInspectionPage, getInspection, saveInspection, updateInspection, recheckInspection,
  getRecheckDetail
} from '@/api/inspection'
import { getSchedulePage, getScheduleDetail, checkWeldSeamLocked, getWeldSeamReportBySeamNo } from '@/api/workstation'
import { getWorkstationList } from '@/api/workstation'
import { getWelderList } from '@/api/welder'
import { getIssuedProcessCardList } from '@/api/processCard'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const searchForm = reactive({ scheduleId: null, welderId: null, overallResult: null })

const stationList = ref([])
const welderList = ref([])
const issuedCardList = ref([])
const inspectingList = ref([])

const showLockedList = ref(false)

const getStationName = (id) => stationList.value.find(s => s.id === id)?.stationName || ''
const getWelderName = (id) => welderList.value.find(w => w.id === id)?.welderName || ''
const getCardName = (id) => issuedCardList.value.find(c => c.id === id)?.cardName || ''

const lockedInspections = computed(() =>
  tableData.value.filter(r => r.overallResult === 0 && r._weldSeamLocked === true)
)
const lockedCount = computed(() => lockedInspections.value.length)

const dialogVisible = ref(false)
const isView = ref(false)
const formRef = ref()
const form = reactive({
  id: null, scheduleId: null, workstationId: null, welderId: null, processCardId: null,
  appearanceCheck: null, sizeCheck: null, ndtCheck: null, overallResult: null,
  defectDesc: '', rectificationAdvice: '', recheckRequired: 0, remark: ''
})

const viewWeldSeamNo = ref('')
const viewWeldSeamLocked = ref(false)
const viewWeldSeamReports = ref([])

const recheckDialog = ref(false)
const recheckRow = ref(null)
const recheckResult = ref(1)
const recheckDetail = reactive({
  machineNo: '',
  materialBatch: '',
  repairReason: '',
  weldSeamNo: '',
  weldSeamLocked: false,
  weldSeamReports: []
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getInspectionPage({
      ...searchForm,
      pageNum: page.pageNum,
      pageSize: page.pageSize
    })
    tableData.value = res?.records || []
    total.value = res?.total || 0
    await loadWeldSeamLockStatus()
  } finally {
    loading.value = false
  }
}

const loadWeldSeamLockStatus = async () => {
  const failedRows = tableData.value.filter(r => r.overallResult === 0 && r.scheduleId)
  if (failedRows.length === 0) return
  for (const row of failedRows) {
    try {
      const detail = await getScheduleDetail(row.scheduleId)
      const seamNo = detail?.schedule?.weldSeamNo || detail?.weldSeamNo
      if (seamNo) {
        row._weldSeamNo = seamNo
        const lockRes = await checkWeldSeamLocked(seamNo)
        row._weldSeamLocked = !!lockRes
      } else {
        row._weldSeamLocked = undefined
      }
    } catch (e) {
      row._weldSeamLocked = undefined
    }
  }
}

const loadInspecting = async () => {
  try {
    const res = await getSchedulePage({ scheduleStatus: 'INSPECTING', pageNum: 1, pageSize: 50 })
    inspectingList.value = res?.records || []
  } catch (e) {}
}

const loadBaseData = async () => {
  try { stationList.value = await getWorkstationList() || [] } catch (e) {}
  try { welderList.value = await getWelderList() || [] } catch (e) {}
  try { issuedCardList.value = await getIssuedProcessCardList() || [] } catch (e) {}
}

const resetSearch = () => {
  Object.assign(searchForm, { scheduleId: null, welderId: null, overallResult: null })
  loadData()
}

const resetForm = () => {
  Object.assign(form, {
    id: null, scheduleId: null, workstationId: null, welderId: null, processCardId: null,
    appearanceCheck: null, sizeCheck: null, ndtCheck: null, overallResult: null,
    defectDesc: '', rectificationAdvice: '', recheckRequired: 0, remark: ''
  })
  viewWeldSeamNo.value = ''
  viewWeldSeamLocked.value = false
  viewWeldSeamReports.value = []
}

const resetRecheckDetail = () => {
  Object.assign(recheckDetail, {
    machineNo: '',
    materialBatch: '',
    repairReason: '',
    weldSeamNo: '',
    weldSeamLocked: false,
    weldSeamReports: []
  })
}

const handleNewInspection = (schedule) => {
  isView.value = false
  resetForm()
  form.scheduleId = schedule.id
  form.workstationId = schedule.workstationId
  form.welderId = schedule.welderId
  form.processCardId = schedule.processCardId
  dialogVisible.value = true
}

const handleView = async (row) => {
  isView.value = true
  viewWeldSeamNo.value = ''
  viewWeldSeamLocked.value = false
  viewWeldSeamReports.value = []
  const data = await getInspection(row.id)
  Object.assign(form, data)
  if (row.overallResult === 0 && row.scheduleId) {
    try {
      const detail = await getRecheckDetail(row.id)
      const seamNo = detail?.weldSeamNo || ''
      if (seamNo) {
        viewWeldSeamNo.value = seamNo
        viewWeldSeamLocked.value = !!detail?.weldSeamLocked
        viewWeldSeamReports.value = detail?.weldSeamReports || []
      }
    } catch (e) {}
  }
  dialogVisible.value = true
}

const submitForm = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    if (form.id) {
      await updateInspection(form)
    } else {
      await saveInspection(form, 5)
    }
    ElMessage.success('提交成功')
    dialogVisible.value = false
    loadData()
    loadInspecting()
  })
}

const handleRecheck = async (row) => {
  recheckRow.value = row
  recheckResult.value = 1
  resetRecheckDetail()
  try {
    const detail = await getRecheckDetail(row.id)
    recheckDetail.machineNo = detail?.machineNo || ''
    recheckDetail.materialBatch = detail?.materialBatch || ''
    recheckDetail.repairReason = detail?.repairReason || ''
    recheckDetail.weldSeamNo = detail?.weldSeamNo || ''
    recheckDetail.weldSeamLocked = !!detail?.weldSeamLocked
    recheckDetail.weldSeamReports = detail?.weldSeamReports || []
  } catch (e) {}
  recheckDialog.value = true
}

const submitRecheck = async () => {
  await recheckInspection(recheckRow.value.id, 5, recheckResult.value)
  ElMessage.success(recheckResult.value === 1 ? '复检合格，焊缝报工已解锁' : '复检结果已提交')
  recheckDialog.value = false
  loadData()
}

onMounted(async () => {
  await loadBaseData()
  loadData()
  loadInspecting()
})
</script>

<style scoped>
.header-badge {
  margin-left: 16px;
}
</style>
