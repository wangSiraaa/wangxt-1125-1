<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">工艺卡管理</span>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增工艺卡
      </el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索工艺卡编号/名称/项目" clearable style="width: 280px" @keyup.enter="loadData" />
      <el-select v-model="searchForm.cardStatus" placeholder="状态" clearable style="width: 140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已审批" value="APPROVED" />
        <el-option label="已下发" value="ISSUED" />
        <el-option label="已作废" value="OBSOLETE" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="cardNo" label="工艺卡编号" width="150" />
      <el-table-column prop="cardName" label="工艺卡名称" width="200" show-overflow-tooltip />
      <el-table-column prop="projectName" label="所属项目/分段" width="180" show-overflow-tooltip />
      <el-table-column prop="workSection" label="工序" width="120" />
      <el-table-column prop="material" label="母材材质" width="120" />
      <el-table-column prop="materialSpec" label="规格" width="100" />
      <el-table-column prop="weldMethod" label="焊接方法" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="cardStatusType(row.cardStatus)" effect="dark" size="small">
            {{ cardStatusText(row.cardStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="issueTime" label="下发时间" width="170" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" link @click="handleView(row)">查看</el-button>
            <el-button v-if="row.cardStatus === 'DRAFT'" size="small" type="success" link @click="handleApprove(row)">审批</el-button>
            <el-button v-if="row.cardStatus === 'DRAFT' || row.cardStatus === 'APPROVED'" size="small" type="warning" link @click="handleIssue(row)">下发</el-button>
            <el-button v-if="row.cardStatus === 'DRAFT'" size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isView ? '工艺卡详情' : (isEdit ? '编辑工艺卡' : '新增工艺卡')" width="800px" destroy-on-close>
      <el-form :model="form" label-width="120px" ref="formRef" :disabled="isView">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工艺卡编号" prop="cardNo" :rules="{ required: true, message: '请输入工艺卡编号' }">
              <el-input v-model="form.cardNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工艺卡名称" prop="cardName" :rules="{ required: true, message: '请输入工艺卡名称' }">
              <el-input v-model="form.cardName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属项目/分段">
              <el-input v-model="form.projectName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序/工步">
              <el-input v-model="form.workSection" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="母材材质">
              <el-input v-model="form.material" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="母材规格">
              <el-input v-model="form.materialSpec" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="焊接方法">
              <el-select v-model="form.weldMethod" placeholder="请选择">
                <el-option label="GTAW钨极氩弧焊" value="GTAW" />
                <el-option label="SMAW焊条电弧焊" value="SMAW" />
                <el-option label="GMAW熔化极气体保护焊" value="GMAW" />
                <el-option label="SAW埋弧焊" value="SAW" />
                <el-option label="GTAW+SMAW组合焊" value="GTAW+SMAW" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="焊接材料">
              <el-input v-model="form.weldMaterial" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="焊接电流">
              <el-input v-model="form.weldCurrent" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="焊接电压">
              <el-input v-model="form.weldVoltage" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="焊接速度">
              <el-input v-model="form.weldSpeed" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预热温度">
              <el-input v-model="form.preheatTemp" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="层间温度">
              <el-input v-model="form.interpassTemp" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保护气体">
              <el-input v-model="form.gasType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="气体流量">
              <el-input v-model="form.gasFlow" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质量标准">
              <el-input v-model="form.qualityStandard" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="技术要求">
              <el-input v-model="form.techRequirement" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ isView ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!isView" type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProcessCardPage, saveProcessCard, updateProcessCard, deleteProcessCard,
  approveProcessCard, issueProcessCard
} from '@/api/processCard'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const searchForm = reactive({ keyword: '', cardStatus: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const isView = ref(false)
const formRef = ref()
const form = reactive({
  id: null, cardNo: '', cardName: '', projectName: '', workSection: '',
  material: '', materialSpec: '', weldMethod: '', weldMaterial: '',
  weldCurrent: '', weldVoltage: '', weldSpeed: '', preheatTemp: '',
  interpassTemp: '', gasType: '', gasFlow: '', techRequirement: '',
  qualityStandard: '', cardStatus: 'DRAFT', remark: ''
})

const cardStatusText = (s) => ({ DRAFT: '草稿', APPROVED: '已审批', ISSUED: '已下发', OBSOLETE: '已作废' })[s] || s
const cardStatusType = (s) => ({ DRAFT: 'info', APPROVED: '', ISSUED: 'success', OBSOLETE: 'danger' })[s] || ''

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProcessCardPage({ ...searchForm, pageNum: page.pageNum, pageSize: page.pageSize })
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.cardStatus = ''
  loadData()
}

const resetForm = () => {
  Object.assign(form, {
    id: null, cardNo: '', cardName: '', projectName: '', workSection: '',
    material: '', materialSpec: '', weldMethod: '', weldMaterial: '',
    weldCurrent: '', weldVoltage: '', weldSpeed: '', preheatTemp: '',
    interpassTemp: '', gasType: '', gasFlow: '', techRequirement: '',
    qualityStandard: '', cardStatus: 'DRAFT', remark: ''
  })
}

const handleAdd = () => {
  isEdit.value = false
  isView.value = false
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  isView.value = false
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleView = (row) => {
  isEdit.value = false
  isView.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const submitForm = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    isEdit.value ? await updateProcessCard(form) : await saveProcessCard(form)
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该工艺卡吗？', '提示', { type: 'warning' }).then(async () => {
    await deleteProcessCard(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleApprove = (row) => {
  ElMessageBox.confirm('确定要审批通过该工艺卡吗？', '提示', { type: 'warning' }).then(async () => {
    await approveProcessCard(row.id)
    ElMessage.success('审批成功')
    loadData()
  })
}

const handleIssue = (row) => {
  ElMessageBox.confirm('确定要下发该工艺卡吗？下发后焊工才能查看。', '提示', { type: 'warning' }).then(async () => {
    await issueProcessCard(row.id, 2)
    ElMessage.success('下发成功')
    loadData()
  })
}

onMounted(loadData)
</script>
