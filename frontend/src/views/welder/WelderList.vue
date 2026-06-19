<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">焊工资格管理</span>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增焊工
      </el-button>
    </div>

    <div class="search-bar">
      <el-input v-model="searchForm.keyword" placeholder="搜索焊工编号/姓名" clearable style="width: 240px" @keyup.enter="loadData" />
      <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="在岗" :value="1" />
        <el-option label="离岗" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="welderNo" label="焊工编号" width="120" />
      <el-table-column prop="welderName" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="idCard" label="身份证号" width="200" />
      <el-table-column prop="entryDate" label="入职日期" width="120" />
      <el-table-column label="资格状态" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.certValid" type="success" effect="dark" size="small">资格有效</el-tag>
          <el-tag v-else type="danger" effect="dark" size="small">资格无效</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '在岗' : '离岗' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" link @click="handleViewCerts(row)">
              <el-icon><Document /></el-icon>
              证书
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑焊工' : '新增焊工'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px" ref="formRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="焊工编号" prop="welderNo" :rules="{ required: true, message: '请输入焊工编号' }">
              <el-input v-model="form.welderNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="welderName" :rules="{ required: true, message: '请输入姓名' }">
              <el-input v-model="form.welderName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" placeholder="请选择">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期">
              <el-date-picker v-model="form.entryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="在岗" inactive-text="离岗" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="certDialogVisible" title="焊工资格证书" width="900px" destroy-on-close>
      <div style="margin-bottom: 12px; display: flex; justify-content: space-between;">
        <span style="font-weight: 600;">{{ currentWelder?.welderName }} 的资格证书</span>
        <el-button type="primary" size="small" @click="handleAddCert">
          <el-icon><Plus /></el-icon>
          新增证书
        </el-button>
      </div>
      <el-table :data="certList" size="small" border>
        <el-table-column prop="certNo" label="证书编号" width="150" />
        <el-table-column prop="certType" label="证书类型" width="200" />
        <el-table-column prop="certLevel" label="等级" width="80" />
        <el-table-column prop="materialSpec" label="认可材质" width="140" />
        <el-table-column prop="thicknessRange" label="厚度范围" width="120" />
        <el-table-column prop="weldPosition" label="焊接位置" width="120" />
        <el-table-column prop="issueDate" label="发证日期" width="120" />
        <el-table-column prop="expiryDate" label="到期日期" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.certStatus === 1" type="success" effect="dark" size="small">有效</el-tag>
            <el-tag v-else-if="row.certStatus === 2" type="warning" effect="dark" size="small">即将过期</el-tag>
            <el-tag v-else type="danger" effect="dark" size="small">已过期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="handleDeleteCert(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="certFormVisible" :title="isCertEdit ? '编辑证书' : '新增证书'" width="600px" destroy-on-close>
      <el-form :model="certForm" label-width="110px" ref="certFormRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="证书编号" prop="certNo" :rules="{ required: true, message: '请输入证书编号' }">
              <el-input v-model="certForm.certNo" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证书类型" prop="certType" :rules="{ required: true, message: '请输入证书类型' }">
              <el-select v-model="certForm.certType" placeholder="请选择">
                <el-option label="GTAW钨极氩弧焊" value="GTAW钨极氩弧焊" />
                <el-option label="SMAW焊条电弧焊" value="SMAW焊条电弧焊" />
                <el-option label="GMAW熔化极气体保护焊" value="GMAW熔化极气体保护焊" />
                <el-option label="SAW埋弧焊" value="SAW埋弧焊" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资格等级">
              <el-select v-model="certForm.certLevel">
                <el-option label="I级" value="I级" />
                <el-option label="II级" value="II级" />
                <el-option label="III级" value="III级" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="认可材质">
              <el-input v-model="certForm.materialSpec" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="厚度范围">
              <el-input v-model="certForm.thicknessRange" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="焊接位置">
              <el-input v-model="certForm.weldPosition" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发证日期" prop="issueDate" :rules="{ required: true, message: '请选择发证日期' }">
              <el-date-picker v-model="certForm.issueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="expiryDate" :rules="{ required: true, message: '请选择到期日期' }">
              <el-date-picker v-model="certForm.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="发证机构">
              <el-input v-model="certForm.issueOrg" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="certFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCertForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Document } from '@element-plus/icons-vue'
import {
  getWelderPage, saveWelder, updateWelder, deleteWelder,
  getWelderCertificates, saveCertificate, deleteCertificate, checkWelderCertValid
} from '@/api/welder'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const searchForm = reactive({ keyword: '', status: null })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  welderNo: '',
  welderName: '',
  gender: '',
  idCard: '',
  entryDate: '',
  status: 1,
  remark: ''
})

const certDialogVisible = ref(false)
const certFormVisible = ref(false)
const isCertEdit = ref(false)
const certFormRef = ref()
const currentWelder = ref()
const certList = ref([])
const certForm = reactive({
  id: null,
  welderId: null,
  certNo: '',
  certType: '',
  certLevel: '',
  materialSpec: '',
  thicknessRange: '',
  weldPosition: '',
  issueDate: '',
  expiryDate: '',
  issueOrg: '',
  certStatus: 1
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getWelderPage({
  ...searchForm,
  pageNum: page.pageNum,
  pageSize: page.pageSize
})
    tableData.value = res?.records || []
    total.value = res?.total || 0
    for (const row of tableData.value) {
      try {
        row.certValid = await checkWelderCertValid(row.id)
      } catch (e) { row.certValid = false }
    }
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = null
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, { id: null, welderNo: '', welderName: '', gender: '', idCard: '', entryDate: '', status: 1, remark: '' })
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
    if (isEdit.value) {
      await updateWelder(form)
    } else {
      await saveWelder(form)
    }
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该焊工吗？', '提示', { type: 'warning' }).then(async () => {
    await deleteWelder(row.id)
    ElMessage.success('删除成功')
    loadData()
  })
}

const handleViewCerts = async (row) => {
  currentWelder.value = row
  certList.value = await getWelderCertificates(row.id) || []
  certDialogVisible.value = true
}

const handleAddCert = () => {
  isCertEdit.value = false
  Object.assign(certForm, {
    id: null, welderId: currentWelder.value.id, certNo: '', certType: '', certLevel: '',
    materialSpec: '', thicknessRange: '', weldPosition: '',
    issueDate: '', expiryDate: '', issueOrg: '', certStatus: 1
  })
  certFormVisible.value = true
}

const submitCertForm = async () => {
  certFormRef.value.validate(async (valid) => {
    if (!valid) return
    await saveCertificate(certForm)
    ElMessage.success('保存成功')
    certFormVisible.value = false
    certList.value = await getWelderCertificates(currentWelder.value.id) || []
    loadData()
  })
}

const handleDeleteCert = (row) => {
  ElMessageBox.confirm('确定要删除该证书吗？', '提示', { type: 'warning' }).then(async () => {
    await deleteCertificate(row.id)
    ElMessage.success('删除成功')
    certList.value = await getWelderCertificates(currentWelder.value.id) || []
    loadData()
  })
}

onMounted(loadData)
</script>
