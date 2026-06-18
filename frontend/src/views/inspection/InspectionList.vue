<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">首件质检</span>
    </div>

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
      <el-table-column prop="inspectionTime" label="检查时间" width="170" />
      <el-table-column label="复检" width="140">
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
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" link @click="handleView(row)">查看</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isView ? '首件检查详情' : '提交首件检查'" width="720px" destroy-on-close>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ isView ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!isView" type="primary" @click="submitForm">提交检查结果</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recheckDialog" title="复检" width="400px" destroy-on-close>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getInspectionPage, getInspection, saveInspection, updateInspection, recheckInspection
} from '@/api/inspection'
import { getSchedulePage } from '@/api/workstation'
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

const getStationName = (id) => stationList.value.find(s => s.id === id)?.stationName || ''
const getWelderName = (id) => welderList.value.find(w => w.id === id)?.welderName || ''
const getCardName = (id) => issuedCardList.value.find(c => c.id === id)?.cardName || ''

const dialogVisible = ref(false)
const isView = ref(false)
const formRef = ref()
const form = reactive({
  id: null, scheduleId: null, workstationId: null, welderId: null, processCardId: null,
  appearanceCheck: null, sizeCheck: null, ndtCheck: null, overallResult: null,
  defectDesc: '', rectificationAdvice: '', recheckRequired: 0, remark: ''
})

const recheckDialog = ref(false)
const recheckRow = ref(null)
const recheckResult = ref(1)

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
  } finally {
    loading.value = false
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
  const data = await getInspection(row.id)
  Object.assign(form, data)
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

const handleRecheck = (row) => {
  recheckRow.value = row
  recheckResult.value = 1
  recheckDialog.value = true
}

const submitRecheck = async () => {
  await recheckInspection(recheckRow.value.id, 5, recheckResult.value)
  ElMessage.success('复检结果已提交')
  recheckDialog.value = false
  loadData()
}

onMounted(async () => {
  await loadBaseData()
  loadData()
  loadInspecting()
})
</script>
