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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkScheduleServiceImpl extends ServiceImpl<WorkScheduleMapper, WorkSchedule> implements WorkScheduleService {

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
        this.save(schedule);

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

        boolean canStart = certValid && processConfirmed && safetyPassed;
        result.put("canStart", canStart);

        StringBuilder msg = new StringBuilder();
        if (!certValid) msg.append("焊工资格已过期；");
        if (!processConfirmed) msg.append("工艺卡尚未确认；");
        if (!safetyPassed) msg.append("安全检查未通过；");
        result.put("message", canStart ? "满足开工条件" : msg.toString());

        return result;
    }
}
