package com.shipyard.welding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shipyard.welding.entity.*;
import com.shipyard.welding.exception.BusinessException;
import com.shipyard.welding.mapper.*;
import com.shipyard.welding.service.WorkScheduleService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkScheduleServiceImpl extends ServiceImpl<WorkScheduleMapper, WorkSchedule> implements WorkScheduleService {

    private static final int CERT_EXPIRING_WARNING_DAYS = 90;

    @Autowired
    private WorkstationMapper workstationMapper;

    @Autowired
    private WelderMapper welderMapper;

    @Autowired
    private WelderCertificateMapper certificateMapper;

    @Autowired
    private ProcessCardMapper processCardMapper;

    @Autowired
    private ProcessConfirmMapper processConfirmMapper;

    @Autowired
    private SafetyCheckMapper safetyCheckMapper;

    @Autowired
    private FirstArticleInspectionMapper inspectionMapper;

    @Autowired
    private HotWorkPermitMapper hotWorkPermitMapper;

    @Autowired
    private ProcessDeviationMapper processDeviationMapper;

    @Autowired
    private SafetyReviewMapper safetyReviewMapper;

    @Autowired
    private WeldSeamReportMapper weldSeamReportMapper;

    @Override
    public IPage<WorkSchedule> queryPage(Map<String, Object> params) {
        Page<WorkSchedule> page = new Page<>(
                Long.parseLong(params.getOrDefault("pageNum", "1").toString()),
                Long.parseLong(params.getOrDefault("pageSize", "10").toString())
        );
        LambdaQueryWrapper<WorkSchedule> wrapper = new LambdaQueryWrapper<>();
        if (params.get("workstationId") != null) {
            wrapper.eq(WorkSchedule::getWorkstationId, params.get("workstationId"));
        }
        if (params.get("welderId") != null) {
            wrapper.eq(WorkSchedule::getWelderId, params.get("welderId"));
        }
        if (params.get("scheduleDate") != null && StringUtils.isNotBlank(params.get("scheduleDate").toString())) {
            wrapper.eq(WorkSchedule::getScheduleDate, LocalDate.parse(params.get("scheduleDate").toString()));
        }
        if (params.get("scheduleStatus") != null && StringUtils.isNotBlank(params.get("scheduleStatus").toString())) {
            wrapper.eq(WorkSchedule::getScheduleStatus, params.get("scheduleStatus"));
        }
        wrapper.orderByDesc(WorkSchedule::getScheduleDate).orderByDesc(WorkSchedule::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional
    public String createSchedule(WorkSchedule schedule, Long teamLeaderId) {
        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws == null) {
            throw new BusinessException("工位不存在");
        }
        if (!"IDLE".equals(ws.getCurrentStatus())) {
            throw new BusinessException("工位当前状态为[" + ws.getCurrentStatus() + "]，无法排班");
        }
        Welder welder = welderMapper.selectById(schedule.getWelderId());
        if (welder == null) {
            throw new BusinessException("焊工不存在");
        }
        ProcessCard card = processCardMapper.selectById(schedule.getProcessCardId());
        if (card == null) {
            throw new BusinessException("工艺卡不存在");
        }
        if (!"ISSUED".equals(card.getCardStatus())) {
            throw new BusinessException("工艺卡未下发，无法排班");
        }

        String scheduleNo = "S" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        schedule.setScheduleNo(scheduleNo);
        schedule.setScheduleStatus("SCHEDULED");
        schedule.setTeamLeaderId(teamLeaderId);

        boolean needSafetyReview = checkSafetyReviewRequired(schedule, card);
        schedule.setRequireSafetyReview(needSafetyReview ? 1 : 0);

        this.save(schedule);

        if (needSafetyReview) {
            createSafetyReview(schedule, card);
        }

        ws.setCurrentStatus("SCHEDULED");
        workstationMapper.updateById(ws);

        ProcessConfirm confirm = new ProcessConfirm();
        confirm.setScheduleId(schedule.getId());
        confirm.setWelderId(schedule.getWelderId());
        confirm.setProcessCardId(schedule.getProcessCardId());
        confirm.setConfirmStatus("PENDING");
        processConfirmMapper.insert(confirm);

        return scheduleNo;
    }

    private boolean checkSafetyReviewRequired(WorkSchedule schedule, ProcessCard card) {
        boolean certExpiring = hasCertExpiring(schedule.getWelderId());
        boolean heightWork = hasHeightWorkOnWorkstation(schedule.getWorkstationId());
        boolean hotWorkRisk = hasActiveHotWorkPermit(schedule.getWorkstationId());
        boolean deviation = hasApprovedDeviation(schedule.getProcessCardId());
        return certExpiring || heightWork || hotWorkRisk || deviation;
    }

    private boolean hasCertExpiring(Long welderId) {
        LocalDate warningDate = LocalDate.now().plusDays(CERT_EXPIRING_WARNING_DAYS);
        Long count = certificateMapper.selectCount(
                new LambdaQueryWrapper<WelderCertificate>()
                        .eq(WelderCertificate::getWelderId, welderId)
                        .in(WelderCertificate::getCertStatus, 1, 2)
                        .le(WelderCertificate::getExpiryDate, warningDate)
                        .ge(WelderCertificate::getExpiryDate, LocalDate.now())
        );
        return count != null && count > 0;
    }

    private boolean hasHeightWorkOnWorkstation(Long workstationId) {
        Long count = safetyCheckMapper.selectCount(
                new LambdaQueryWrapper<SafetyCheck>()
                        .eq(SafetyCheck::getWorkstationId, workstationId)
                        .like(SafetyCheck::getCheckItem, "高处")
        );
        if (count != null && count > 0) return true;

        Long hotCount = hotWorkPermitMapper.selectCount(
                new LambdaQueryWrapper<HotWorkPermit>()
                        .eq(HotWorkPermit::getWorkstationId, workstationId)
                        .eq(HotWorkPermit::getPermitStatus, "APPROVED")
        );
        return hotCount != null && hotCount > 0;
    }

    private boolean hasActiveHotWorkPermit(Long workstationId) {
        Long count = hotWorkPermitMapper.selectCount(
                new LambdaQueryWrapper<HotWorkPermit>()
                        .eq(HotWorkPermit::getWorkstationId, workstationId)
                        .eq(HotWorkPermit::getPermitStatus, "APPROVED")
                        .ge(HotWorkPermit::getWorkEndTime, LocalDateTime.now())
        );
        return count != null && count > 0;
    }

    private boolean hasApprovedDeviation(Long processCardId) {
        Long count = processDeviationMapper.selectCount(
                new LambdaQueryWrapper<ProcessDeviation>()
                        .eq(ProcessDeviation::getProcessCardId, processCardId)
                        .eq(ProcessDeviation::getDeviationStatus, "APPROVED")
        );
        return count != null && count > 0;
    }

    private void createSafetyReview(WorkSchedule schedule, ProcessCard card) {
        List<String> triggerTypes = new ArrayList<>();
        List<String> details = new ArrayList<>();
        boolean hasHeightWork = false;
        boolean hasHotWork = false;
        boolean hasDeviation = false;
        List<String> expiringCertIds = new ArrayList<>();

        LocalDate warningDate = LocalDate.now().plusDays(CERT_EXPIRING_WARNING_DAYS);
        List<WelderCertificate> expiringCerts = certificateMapper.selectList(
                new LambdaQueryWrapper<WelderCertificate>()
                        .eq(WelderCertificate::getWelderId, schedule.getWelderId())
                        .in(WelderCertificate::getCertStatus, 1, 2)
                        .le(WelderCertificate::getExpiryDate, warningDate)
                        .ge(WelderCertificate::getExpiryDate, LocalDate.now())
        );
        if (!expiringCerts.isEmpty()) {
            triggerTypes.add("CERT_EXPIRING");
            expiringCertIds = expiringCerts.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.toList());
            details.add("焊工证书即将过期：" + expiringCerts.stream()
                    .map(c -> c.getCertNo() + "(" + c.getCertType() + "到期:" + c.getExpiryDate() + ")")
                    .collect(Collectors.joining("；")));
        }

        Long heightCount = safetyCheckMapper.selectCount(
                new LambdaQueryWrapper<SafetyCheck>()
                        .eq(SafetyCheck::getWorkstationId, schedule.getWorkstationId())
                        .like(SafetyCheck::getCheckItem, "高处")
        );
        if (heightCount != null && heightCount > 0) {
            triggerTypes.add("HEIGHT_WORK");
            hasHeightWork = true;
            details.add("该工位同时存在高处作业");
        }

        Long hotCount = hotWorkPermitMapper.selectCount(
                new LambdaQueryWrapper<HotWorkPermit>()
                        .eq(HotWorkPermit::getWorkstationId, schedule.getWorkstationId())
                        .eq(HotWorkPermit::getPermitStatus, "APPROVED")
        );
        if (hotCount != null && hotCount > 0) {
            triggerTypes.add("HOT_WORK_RISK");
            hasHotWork = true;
            details.add("该工位存在有效的动火票，涉及动火作业风险");
        }

        Long devCount = processDeviationMapper.selectCount(
                new LambdaQueryWrapper<ProcessDeviation>()
                        .eq(ProcessDeviation::getProcessCardId, schedule.getProcessCardId())
                        .eq(ProcessDeviation::getDeviationStatus, "APPROVED")
        );
        if (devCount != null && devCount > 0) {
            triggerTypes.add("DEVIATION_EXISTS");
            hasDeviation = true;
            details.add("该工艺卡存在已批准的工艺偏离");
        }

        SafetyReview review = new SafetyReview();
        review.setReviewNo("SR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        review.setScheduleId(schedule.getId());
        review.setWorkstationId(schedule.getWorkstationId());
        review.setWelderId(schedule.getWelderId());
        review.setTriggerType(String.join(",", triggerTypes));
        review.setTriggerDetail(String.join("；", details));
        review.setCertExpiringIds(String.join(",", expiringCertIds));
        review.setHasHeightWork(hasHeightWork ? 1 : 0);
        review.setHasHotWork(hasHotWork ? 1 : 0);
        review.setHasDeviation(hasDeviation ? 1 : 0);
        review.setReviewStatus("PENDING");
        safetyReviewMapper.insert(review);

        schedule.setSafetyReviewId(review.getId());
        this.updateById(schedule);
    }

    @Override
    @Transactional
    public void startWork(Long scheduleId, Long welderId) {
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        if (!"SCHEDULED".equals(schedule.getScheduleStatus()) && !"SUSPENDED".equals(schedule.getScheduleStatus())) {
            throw new BusinessException("当前排班状态为[" + schedule.getScheduleStatus() + "]，无法开工");
        }

        int validCount = certificateMapper.countValidCerts(schedule.getWelderId(), LocalDate.now());
        if (validCount == 0) {
            throw new BusinessException("焊工资格已过期，无法开工！请先更新资格证书。");
        }

        ProcessConfirm confirm = processConfirmMapper.selectOne(
                new LambdaQueryWrapper<ProcessConfirm>().eq(ProcessConfirm::getScheduleId, scheduleId)
        );
        if (confirm == null || !"CONFIRMED".equals(confirm.getConfirmStatus())) {
            throw new BusinessException("工艺卡尚未确认，无法开工！请焊工先确认工艺卡。");
        }

        List<SafetyCheck> checks = safetyCheckMapper.selectList(
                new LambdaQueryWrapper<SafetyCheck>().eq(SafetyCheck::getScheduleId, scheduleId)
        );
        boolean allPassed = checks.stream().allMatch(c -> c.getCheckResult() != null && c.getCheckResult() == 1);
        if (!checks.isEmpty() && !allPassed) {
            throw new BusinessException("存在不合格的安全检查项，无法开工！");
        }

        if (schedule.getHotWorkPermitId() != null) {
            HotWorkPermit permit = hotWorkPermitMapper.selectById(schedule.getHotWorkPermitId());
            if (permit != null && !"APPROVED".equals(permit.getPermitStatus())) {
                throw new BusinessException("关联的动火票未获批准，无法开工！");
            }
        }

        if (schedule.getRequireSafetyReview() != null && schedule.getRequireSafetyReview() == 1) {
            if (schedule.getSafetyReviewId() != null) {
                SafetyReview review = safetyReviewMapper.selectById(schedule.getSafetyReviewId());
                if (review == null || !"APPROVED".equals(review.getReviewStatus())) {
                    throw new BusinessException("安全员尚未复核通过，无法开工！请等待安全员复核。");
                }
            } else {
                throw new BusinessException("该排班需要安全员复核但尚未生成复核单，无法开工！");
            }
        }

        schedule.setScheduleStatus("WORKING");
        schedule.setActualStartTime(LocalDateTime.now());
        this.updateById(schedule);

        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws != null) {
            ws.setCurrentStatus("WORKING");
            workstationMapper.updateById(ws);
        }
    }

    @Override
    @Transactional
    public void confirmProcessCard(ProcessConfirm confirm) {
        ProcessConfirm dbConfirm = processConfirmMapper.selectOne(
                new LambdaQueryWrapper<ProcessConfirm>()
                        .eq(ProcessConfirm::getScheduleId, confirm.getScheduleId())
                        .eq(ProcessConfirm::getWelderId, confirm.getWelderId())
        );
        if (dbConfirm == null) {
            dbConfirm = new ProcessConfirm();
            dbConfirm.setScheduleId(confirm.getScheduleId());
            dbConfirm.setWelderId(confirm.getWelderId());
            dbConfirm.setProcessCardId(confirm.getProcessCardId());
        }
        dbConfirm.setConfirmStatus(confirm.getConfirmStatus());
        if ("REJECTED".equals(confirm.getConfirmStatus())) {
            dbConfirm.setRejectReason(confirm.getRejectReason());
        }
        dbConfirm.setConfirmTime(LocalDateTime.now());
        if (dbConfirm.getId() == null) {
            processConfirmMapper.insert(dbConfirm);
        } else {
            processConfirmMapper.updateById(dbConfirm);
        }
    }

    @Override
    @Transactional
    public void submitFirstInspection(Long scheduleId) {
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        if (!"WORKING".equals(schedule.getScheduleStatus())) {
            throw new BusinessException("当前排班状态为[" + schedule.getScheduleStatus() + "]，无法提交首检");
        }
        schedule.setScheduleStatus("INSPECTING");
        this.updateById(schedule);

        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws != null) {
            ws.setCurrentStatus("INSPECTING");
            workstationMapper.updateById(ws);
        }
    }

    @Override
    @Transactional
    public void completeWork(Long scheduleId) {
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        schedule.setScheduleStatus("COMPLETED");
        schedule.setActualEndTime(LocalDateTime.now());
        this.updateById(schedule);

        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws != null) {
            ws.setCurrentStatus("IDLE");
            workstationMapper.updateById(ws);
        }
    }

    @Override
    @Transactional
    public void suspendWork(Long scheduleId, String reason) {
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        schedule.setScheduleStatus("SUSPENDED");
        schedule.setSuspendReason(reason);
        schedule.setSuspendTime(LocalDateTime.now());
        this.updateById(schedule);

        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws != null) {
            ws.setCurrentStatus("SUSPENDED");
            workstationMapper.updateById(ws);
        }
    }

    @Override
    @Transactional
    public void resumeWork(Long scheduleId) {
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        if (!"SUSPENDED".equals(schedule.getScheduleStatus())) {
            throw new BusinessException("当前状态不是暂停，无法恢复");
        }
        schedule.setScheduleStatus("WORKING");
        schedule.setResumeTime(LocalDateTime.now());
        this.updateById(schedule);

        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws != null) {
            ws.setCurrentStatus("WORKING");
            workstationMapper.updateById(ws);
        }
    }

    @Override
    @Transactional
    public void cancelSchedule(Long scheduleId) {
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        schedule.setScheduleStatus("CANCELLED");
        this.updateById(schedule);

        Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
        if (ws != null) {
            ws.setCurrentStatus("IDLE");
            workstationMapper.updateById(ws);
        }
    }

    @Override
    public List<SafetyCheck> getSafetyChecks(Long scheduleId) {
        return safetyCheckMapper.selectList(
                new LambdaQueryWrapper<SafetyCheck>().eq(SafetyCheck::getScheduleId, scheduleId)
        );
    }

    @Override
    public void saveSafetyChecks(List<SafetyCheck> checks) {
        for (SafetyCheck check : checks) {
            if (check.getId() == null) {
                safetyCheckMapper.insert(check);
            } else {
                safetyCheckMapper.updateById(check);
            }
        }
    }

    @Override
    public Map<String, Object> getStartPermitStatus(Long scheduleId) {
        Map<String, Object> result = new HashMap<>();
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            result.put("canStart", false);
            result.put("message", "排班记录不存在");
            return result;
        }

        int validCount = certificateMapper.countValidCerts(schedule.getWelderId(), LocalDate.now());
        boolean certValid = validCount > 0;
        result.put("certValid", certValid);

        boolean certExpiring = hasCertExpiring(schedule.getWelderId());
        result.put("certExpiring", certExpiring);

        ProcessConfirm confirm = processConfirmMapper.selectOne(
                new LambdaQueryWrapper<ProcessConfirm>().eq(ProcessConfirm::getScheduleId, scheduleId)
        );
        boolean processConfirmed = confirm != null && "CONFIRMED".equals(confirm.getConfirmStatus());
        result.put("processConfirmed", processConfirmed);
        result.put("confirmStatus", confirm != null ? confirm.getConfirmStatus() : "PENDING");

        List<SafetyCheck> checks = safetyCheckMapper.selectList(
                new LambdaQueryWrapper<SafetyCheck>().eq(SafetyCheck::getScheduleId, scheduleId)
        );
        boolean safetyPassed = checks.isEmpty() || checks.stream().allMatch(c -> c.getCheckResult() != null && c.getCheckResult() == 1);
        result.put("safetyPassed", safetyPassed);

        boolean heightWork = hasHeightWorkOnWorkstation(schedule.getWorkstationId());
        result.put("heightWork", heightWork);

        boolean hotWorkPermitValid = false;
        if (schedule.getHotWorkPermitId() != null) {
            HotWorkPermit permit = hotWorkPermitMapper.selectById(schedule.getHotWorkPermitId());
            hotWorkPermitValid = permit != null && "APPROVED".equals(permit.getPermitStatus());
        }
        result.put("hotWorkPermitValid", hotWorkPermitValid);
        result.put("hotWorkPermitId", schedule.getHotWorkPermitId());

        boolean safetyReviewApproved = true;
        if (schedule.getRequireSafetyReview() != null && schedule.getRequireSafetyReview() == 1) {
            if (schedule.getSafetyReviewId() != null) {
                SafetyReview review = safetyReviewMapper.selectById(schedule.getSafetyReviewId());
                safetyReviewApproved = review != null && "APPROVED".equals(review.getReviewStatus());
            } else {
                safetyReviewApproved = false;
            }
        }
        result.put("safetyReviewApproved", safetyReviewApproved);
        result.put("requireSafetyReview", schedule.getRequireSafetyReview());

        boolean deviationExists = hasApprovedDeviation(schedule.getProcessCardId());
        result.put("deviationExists", deviationExists);

        boolean canStart = certValid && processConfirmed && safetyPassed && safetyReviewApproved;
        result.put("canStart", canStart);

        StringBuilder msg = new StringBuilder();
        if (!certValid) msg.append("焊工资格已过期；");
        if (certExpiring) msg.append("焊工证书即将过期(需安全员复核)；");
        if (!processConfirmed) msg.append("工艺卡尚未确认；");
        if (!safetyPassed) msg.append("安全检查未通过；");
        if (heightWork) msg.append("工位存在高处作业(需安全员复核)；");
        if (!hotWorkPermitValid && schedule.getHotWorkPermitId() != null) msg.append("动火票未获批准；");
        if (!safetyReviewApproved) msg.append("安全员尚未复核通过；");
        if (deviationExists) msg.append("存在工艺偏离(需安全员复核)；");
        result.put("message", canStart ? "满足开工条件" : msg.toString());

        return result;
    }

    @Override
    public Map<String, Object> getScheduleDetail(Long scheduleId) {
        Map<String, Object> detail = new HashMap<>();
        WorkSchedule schedule = this.getById(scheduleId);
        if (schedule == null) {
            return detail;
        }
        detail.put("schedule", schedule);

        if (schedule.getWelderId() != null) {
            List<WelderCertificate> certs = certificateMapper.selectByWelderId(schedule.getWelderId());
            detail.put("welderCerts", certs);
        }

        if (schedule.getHotWorkPermitId() != null) {
            HotWorkPermit permit = hotWorkPermitMapper.selectById(schedule.getHotWorkPermitId());
            detail.put("hotWorkPermit", permit);
        }

        if (schedule.getSafetyReviewId() != null) {
            SafetyReview review = safetyReviewMapper.selectById(schedule.getSafetyReviewId());
            detail.put("safetyReview", review);
        }

        List<ProcessDeviation> deviations = processDeviationMapper.selectList(
                new LambdaQueryWrapper<ProcessDeviation>()
                        .eq(ProcessDeviation::getScheduleId, scheduleId)
                        .eq(ProcessDeviation::getDeviationStatus, "APPROVED")
        );
        detail.put("deviations", deviations);

        if (StringUtils.isNotBlank(schedule.getWeldSeamNo())) {
            List<WeldSeamReport> reports = weldSeamReportMapper.selectByWeldSeamNo(schedule.getWeldSeamNo());
            detail.put("weldSeamReports", reports);
            long lockedCount = reports.stream().filter(r -> "LOCKED".equals(r.getWeldStatus())).count();
            detail.put("weldSeamLocked", lockedCount > 0);
        }

        return detail;
    }
}
