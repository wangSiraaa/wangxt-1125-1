<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">排班与开工管理</span>
      <div style="display: flex; gap: 10px;">
        <el-button type="primary" @click="handleAddSchedule">
          <el-icon><Plus /></el-icon>
          新增排班
        </el-button>
        <el-button type="warning" @click="showSafetyReviewPanel">
          <el-icon><Warning /></el-icon>
          安全员复核
          <el-badge v-if="pendingReviewCount > 0" :value="pendingReviewCount" class="badge-dot" />
        </el-button>
      </div>
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
      <el-table-column prop="weldSeamNo" label="焊缝编号" width="120" />
      <el-table-column prop="machineNo" label="机台编号" width="110" />
      <el-table-column prop="materialBatch" label="材料批号" width="110" />
      <el-table-column prop="scheduleDate" label="排班日期" width="120" />
      <el-table-column label="安全复核" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.requireSafetyReview === 1" :type="row.safetyReviewId ? 'success' : 'warning'" size="small" effect="dark">
            需复核
          </el-tag>
          <span v-else style="color: #999; font-size: 12px;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="排班状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.scheduleStatus)" effect="dark" size="small">
            {{ statusText(row.scheduleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="420" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button size="small" type="primary" link @click="checkPermit(row)">
              <el-icon><Key /></el-icon>
              开工检查
            </el-button>
            <el-button size="small" type="info" link @click="showDetail(row)">
              <el-icon><View /></el-icon>
              详情
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

    <el-dialog v-model="scheduleDialog" title="新增排班" width="700px" destroy-on-close>
      <el-form :model="scheduleForm" label-width="110px" ref="scheduleFormRef">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工位" prop="workstationId" :rules="{ required: true, message: '请选择工位' }">
              <el-select v-model="scheduleForm.workstationId" filterable placeholder="请选择空闲工位" style="width: 100%" @change="onWorkstationChange">
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
            <el-form-item label="焊缝编号" prop="weldSeamNo" :rules="{ required: true, message: '请输入焊缝编号' }">
              <el-input v-model="scheduleForm.weldSeamNo" placeholder="如 WS-H1234-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机台编号">
              <el-input v-model="scheduleForm.machineNo" placeholder="如 M-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="材料批号">
              <el-input v-model="scheduleForm.materialBatch" placeholder="如 B2024-001" />
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
            <el-form-item label="关联动火票">
              <el-select v-model="scheduleForm.hotWorkPermitId" clearable placeholder="选填" style="width: 100%">
                <el-option v-for="p in hotWorkPermitOptions" :key="p.id" :label="`${p.permitNo} - ${p.hotWorkType}`" :value="p.id" />
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

    <el-dialog v-model="permitDialog" title="开工许可联动检查" width="640px" destroy-on-close>
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
            <el-tag v-if="permitStatus.certExpiring" type="warning" effect="dark" style="margin-left: 8px;">
              即将过期(需安全员复核)
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
          <el-descriptions-item label="高处作业">
            <el-tag :type="permitStatus.heightWork ? 'warning' : 'success'" effect="dark">
              {{ permitStatus.heightWork ? '存在(需安全员复核)' : '无' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="动火票">
            <el-tag v-if="permitStatus.hotWorkPermitId" :type="permitStatus.hotWorkPermitValid ? 'success' : 'danger'" effect="dark">
              {{ permitStatus.hotWorkPermitValid ? '已批准' : '未批准' }}
            </el-tag>
            <el-tag v-else type="info" effect="dark">无关联动火票</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="工艺偏离">
            <el-tag :type="permitStatus.deviationExists ? 'warning' : 'info'" effect="dark">
              {{ permitStatus.deviationExists ? '存在(需安全员复核)' : '无' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="安全员复核">
            <el-tag v-if="permitStatus.requireSafetyReview" :type="permitStatus.safetyReviewApproved ? 'success' : 'danger'" effect="dark">
              {{ permitStatus.safetyReviewApproved ? '已通过' : '未通过' }}
            </el-tag>
            <el-tag v-else type="info" effect="dark">无需复核</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px; display: flex; gap: 10px;">
          <el-button type="primary" @click="openSafetyCheck">
            <el-icon><Warning /></el-icon>
            安全检查
          </el-button>
          <el-button v-if="permitStatus.requireSafetyReview && !permitStatus.safetyReviewApproved" type="warning" @click="showSafetyReviewFromPermit">
            <el-icon><Warning /></el-icon>
            查看安全员复核
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="开工单详情" width="720px" destroy-on-close>
      <div v-if="scheduleDetail">
        <el-descriptions :column="2" border title="开工单信息">
          <el-descriptions-item label="排班单号">{{ scheduleDetail.schedule?.scheduleNo }}</el-descriptions-item>
          <el-descriptions-item label="焊缝编号">{{ scheduleDetail.schedule?.weldSeamNo }}</el-descriptions-item>
          <el-descriptions-item label="机台编号">{{ scheduleDetail.schedule?.machineNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="材料批号">{{ scheduleDetail.schedule?.materialBatch || '-' }}</el-descriptions-item>
          <el-descriptions-item label="返修原因">{{ scheduleDetail.schedule?.repairReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需要安全复核">{{ scheduleDetail.schedule?.requireSafetyReview ? '是' : '否' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="scheduleDetail.hotWorkPermit" :column="2" border title="关联动火票" style="margin-top: 16px;">
          <el-descriptions-item label="动火票编号">{{ scheduleDetail.hotWorkPermit.permitNo }}</el-descriptions-item>
          <el-descriptions-item label="动火类型">{{ scheduleDetail.hotWorkPermit.hotWorkType }}</el-descriptions-item>
          <el-descriptions-item label="动火等级">{{ scheduleDetail.hotWorkPermit.fireLevel }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ scheduleDetail.hotWorkPermit.permitStatus }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="scheduleDetail.safetyReview" :column="2" border title="安全员复核" style="margin-top: 16px;">
          <el-descriptions-item label="复核单号">{{ scheduleDetail.safetyReview.reviewNo }}</el-descriptions-item>
          <el-descriptions-item label="触发类型">{{ scheduleDetail.safetyReview.triggerType }}</el-descriptions-item>
          <el-descriptions-item label="复核状态">{{ scheduleDetail.safetyReview.reviewStatus }}</el-descriptions-item>
          <el-descriptions-item label="触发详情" :span="2">{{ scheduleDetail.safetyReview.triggerDetail }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="scheduleDetail.weldSeamReports && scheduleDetail.weldSeamReports.length > 0" style="margin-top: 16px;">
          <h4 style="margin-bottom: 8px;">焊缝报工 <el-tag v-if="scheduleDetail.weldSeamLocked" type="danger" size="small" effect="dark">已锁定</el-tag></h4>
          <el-table :data="scheduleDetail.weldSeamReports" border size="small">
            <el-table-column prop="reportNo" label="报工单号" width="160" />
            <el-table-column prop="weldSeamNo" label="焊缝编号" width="120" />
            <el-table-column prop="machineNo" label="机台" width="100" />
            <el-table-column prop="materialBatch" label="材料批号" width="120" />
            <el-table-column prop="weldStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.weldStatus === 'LOCKED' ? 'danger' : row.weldStatus === 'COMPLETED' ? 'success' : ''" size="small">
                  {{ { IN_PROGRESS: '进行中', COMPLETED: '已完成', LOCKED: '已锁定', SUBMITTED: '已提交' }[row.weldStatus] || row.weldStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lockReason" label="锁定原因" show-overflow-tooltip />
          </el-table>
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

    <el-dialog v-model="safetyReviewDialog" title="安全员复核" width="700px" destroy-on-close>
      <el-table :data="pendingReviewList" v-loading="reviewLoading" border>
        <el-table-column prop="reviewNo" label="复核单号" width="170" />
        <el-table-column label="焊工" width="100">
          <template #default="{ row }">{{ getWelderName(row.welderId) }}</template>
        </el-table-column>
        <el-table-column label="触发类型" width="180">
          <template #default="{ row }">
            <el-tag v-for="t in row.triggerType.split(',')" :key="t" size="small" style="margin: 2px;"
              :type="t === 'CERT_EXPIRING' ? 'warning' : t === 'HEIGHT_WORK' ? 'danger' : t === 'HOT_WORK_RISK' ? 'danger' : 'info'">
              {{ { CERT_EXPIRING: '证书即将过期', HEIGHT_WORK: '高处作业并存', HOT_WORK_RISK: '动火风险', DEVIATION_EXISTS: '工艺偏离' }[t] || t }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerDetail" label="触发详情" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleReviewApprove(row)">通过</el-button>
            <el-button type="danger" size="small" @click="handleReviewReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="reviewActionDialog" :title="reviewAction === 'approve' ? '复核通过' : '复核驳回'" width="500px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item v-if="reviewAction === 'approve'" label="补充安全措施">
          <el-input v-model="reviewSafetyMeasures" type="textarea" :rows="3" placeholder="请输入补充安全措施" />
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input v-model="reviewOpinion" type="textarea" :rows="3" placeholder="请输入复核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewActionDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReviewAction">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Key, DocumentChecked, VideoPlay, CircleCheck, Check,
  VideoPause, RefreshRight, Warning, View
} from '@element-plus/icons-vue'
import {
  getSchedulePage, createSchedule, startWork, confirmProcessCard, submitFirstInspection,
  completeWork, suspendWork, resumeWork, cancelSchedule,
  getStartPermitStatus, getSafetyChecks, saveSafetyChecks, getWorkstationList,
  getScheduleDetail, getHotWorkPermitByWorkstation,
  getSafetyReviewPending, approveSafetyReview, rejectSafetyReview
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
const hotWorkPermitOptions = ref([])

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
  shiftType: 'DAY', timeRange: null, taskDesc: '',
  weldSeamNo: '', machineNo: '', materialBatch: '', hotWorkPermitId: null
})

const permitDialog = ref(false)
const permitStatus = ref(null)
const currentSchedule = ref(null)

const detailDialog = ref(false)
const scheduleDetail = ref(null)

const confirmDialog = ref(false)
const confirmResult = ref('CONFIRMED')
const rejectReason = ref('')
const currentCard = ref(null)

const safetyDialog = ref(false)
const safetyCheckList = ref([])

const suspendDialog = ref(false)
const suspendReason = ref('')

const safetyReviewDialog = ref(false)
const pendingReviewList = ref([])
const pendingReviewCount = ref(0)
const reviewLoading = ref(false)

const reviewActionDialog = ref(false)
const reviewAction = ref('approve')
const currentReviewRow = ref(null)
const reviewSafetyMeasures = ref('')
const reviewOpinion = ref('')

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

const loadPendingReviews = async () => {
  try {
    pendingReviewList.value = await getSafetyReviewPending() || []
    pendingReviewCount.value = pendingReviewList.value.length
  } catch (e) {}
}

const resetSearch = () => {
  Object.assign(searchForm, { scheduleDate: '', workstationId: null, welderId: null, scheduleStatus: '' })
  loadData()
}

const onWorkstationChange = async (workstationId) => {
  if (workstationId) {
    try {
      hotWorkPermitOptions.value = await getHotWorkPermitByWorkstation(workstationId) || []
    } catch (e) {
      hotWorkPermitOptions.value = []
    }
  } else {
    hotWorkPermitOptions.value = []
  }
}

const handleAddSchedule = () => {
  Object.assign(scheduleForm, {
    workstationId: null, welderId: null, processCardId: null, scheduleDate: '',
    shiftType: 'DAY', timeRange: null, taskDesc: '',
    weldSeamNo: '', machineNo: '', materialBatch: '', hotWorkPermitId: null
  })
  hotWorkPermitOptions.value = []
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
    loadPendingReviews()
  })
}

const checkPermit = async (row) => {
  currentSchedule.value = row
  permitStatus.value = await getStartPermitStatus(row.id)
  permitDialog.value = true
}

const showDetail = async (row) => {
  const detail = await getScheduleDetail(row.id)
  scheduleDetail.value = detail
  detailDialog.value = true
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

const showSafetyReviewFromPermit = () => {
  permitDialog.value = false
  showSafetyReviewPanel()
}

const showSafetyReviewPanel = async () => {
  await loadPendingReviews()
  safetyReviewDialog.value = true
}

const handleReviewApprove = (row) => {
  currentReviewRow.value = row
  reviewAction.value = 'approve'
  reviewSafetyMeasures.value = ''
  reviewOpinion.value = ''
  reviewActionDialog.value = true
}

const handleReviewReject = (row) => {
  currentReviewRow.value = row
  reviewAction.value = 'reject'
  reviewOpinion.value = ''
  reviewActionDialog.value = true
}

const submitReviewAction = async () => {
  if (reviewAction.value === 'approve') {
    await approveSafetyReview(currentReviewRow.value.id, 5, reviewSafetyMeasures.value, reviewOpinion.value)
    ElMessage.success('复核通过')
  } else {
    if (!reviewOpinion.value.trim()) {
      ElMessage.warning('请输入驳回意见')
      return
    }
    await rejectSafetyReview(currentReviewRow.value.id, 5, reviewOpinion.value)
    ElMessage.success('已驳回')
  }
  reviewActionDialog.value = false
  loadPendingReviews()
  loadData()
}

const handleStart = (row) => {
  ElMessageBox.confirm('开工前将自动检查焊工资格、工艺卡确认、安全检查、动火票及安全员复核状态。', '开工确认', { type: 'warning' }).then(async () => {
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
  loadPendingReviews()
})
</script>
