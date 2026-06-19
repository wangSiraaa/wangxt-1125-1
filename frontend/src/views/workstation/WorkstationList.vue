<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">工位管理</span>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增工位
      </el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索工位编号/名称" clearable style="width: 240px" @keyup.enter="loadData" />
      <el-select v-model="searchForm.currentStatus" placeholder="工位状态" clearable style="width: 140px">
        <el-option label="空闲" value="IDLE" />
        <el-option label="已排班" value="SCHEDULED" />
        <el-option label="开工中" value="WORKING" />
        <el-option label="首检中" value="INSPECTING" />
        <el-option label="已暂停" value="SUSPENDED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="stationNo" label="工位编号" width="120" />
      <el-table-column prop="stationName" label="工位名称" width="180" />
      <el-table-column prop="area" label="所属区域" width="140" />
      <el-table-column prop="equipment" label="设备信息" show-overflow-tooltip />
      <el-table-column label="当前状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.currentStatus)" effect="dark" size="small">
            {{ statusText(row.currentStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑工位' : '新增工位'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px" ref="formRef">
        <el-form-item label="工位编号" prop="stationNo" :rules="{ required: true, message: '请输入工位编号' }">
          <el-input v-model="form.stationNo" />
        </el-form-item>
        <el-form-item label="工位名称" prop="stationName" :rules="{ required: true, message: '请输入工位名称' }">
          <el-input v-model="form.stationName" />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-input v-model="form.area" placeholder="如：船体车间A区" />
        </el-form-item>
        <el-form-item label="设备信息">
          <el-input v-model="form.equipment" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getWorkstationPage, saveWorkstation, updateWorkstation, deleteWorkstation } from '@/api/workstation'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const searchForm = reactive({ keyword: '', currentStatus: '' })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({
  id: null, stationNo: '', stationName: '', area: '', equipment: '', currentStatus: 'IDLE', status: 1, remark: ''
})

const statusText = (s) => ({
  IDLE: '空闲', SCHEDULED: '已排班', WORKING: '开工中', INSPECTING: '首检中', SUSPENDED: '已暂停'
})[s] || s

const statusType = (s) => ({
  IDLE: 'info', SCHEDULED: '', WORKING: 'success', INSPECTING: 'warning', SUSPENDED: 'danger'
})[s] || ''

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWorkstationPage({ ...searchForm, pageNum: page.pageNum, pageSize: page.pageSize })
    tableData.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.currentStatus = ''
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, stationNo: '', stationName: '', area: '', equipment: '', currentStatus: 'IDLE', status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const submitForm = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    isEdit.value ? await updateWorkstation(form) : await saveWorkstation(form)
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该工位吗？', '提示', { type: 'warning' }).then(async () => {
    await deleteWorkstation(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

onMounted(loadData)
</script>
