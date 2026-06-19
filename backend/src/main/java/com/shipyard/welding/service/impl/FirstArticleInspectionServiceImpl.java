package com.shipyard.welding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shipyard.welding.entity.*;
import com.shipyard.welding.exception.BusinessException;
import com.shipyard.welding.mapper.*;
import com.shipyard.welding.service.FirstArticleInspectionService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FirstArticleInspectionServiceImpl extends ServiceImpl<FirstArticleInspectionMapper, FirstArticleInspection> implements FirstArticleInspectionService {

    @Autowired
    private WorkScheduleMapper workScheduleMapper;

    @Autowired
    private WorkstationMapper workstationMapper;

    @Autowired
    private ProcessSuspensionMapper processSuspensionMapper;

    @Autowired
    private WeldSeamReportMapper weldSeamReportMapper;

    @Autowired
    private WelderCertificateMapper welderCertificateMapper;

    @Autowired
    private HotWorkPermitMapper hotWorkPermitMapper;

    @Override
    public IPage<FirstArticleInspection> queryPage(Map<String, Object> params) {
        Page<FirstArticleInspection> page = new Page<>(
                Long.parseLong(params.getOrDefault("pageNum", "1").toString()),
                Long.parseLong(params.getOrDefault("pageSize", "10").toString())
        );
        LambdaQueryWrapper<FirstArticleInspection> wrapper = new LambdaQueryWrapper<>();
        if (params.get("scheduleId") != null) {
            wrapper.eq(FirstArticleInspection::getScheduleId, params.get("scheduleId"));
        }
        if (params.get("welderId") != null) {
            wrapper.eq(FirstArticleInspection::getWelderId, params.get("welderId"));
        }
        if (params.get("overallResult") != null) {
            wrapper.eq(FirstArticleInspection::getOverallResult, params.get("overallResult"));
        }
        wrapper.orderByDesc(FirstArticleInspection::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public FirstArticleInspection getByScheduleId(Long scheduleId) {
        return this.getOne(new LambdaQueryWrapper<FirstArticleInspection>()
                .eq(FirstArticleInspection::getScheduleId, scheduleId)
                .orderByDesc(FirstArticleInspection::getCreateTime)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public String saveInspection(FirstArticleInspection inspection, Long inspectorId) {
        WorkSchedule schedule = workScheduleMapper.selectById(inspection.getScheduleId());
        if (schedule == null) {
            throw new BusinessException("排班记录不存在");
        }
        if (!"INSPECTING".equals(schedule.getScheduleStatus())) {
            throw new BusinessException("当前排班状态不是首检中，无法提交首检记录");
        }

        String inspectionNo = "FI" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        inspection.setInspectionNo(inspectionNo);
        inspection.setInspectorId(inspectorId);
        inspection.setInspectionTime(LocalDateTime.now());
        this.save(inspection);

        if (inspection.getOverallResult() == 0) {
            schedule.setScheduleStatus("SUSPENDED");
            schedule.setSuspendReason("首件检查不合格");
            schedule.setSuspendTime(LocalDateTime.now());
            workScheduleMapper.updateById(schedule);

            Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
            if (ws != null) {
                ws.setCurrentStatus("SUSPENDED");
                workstationMapper.updateById(ws);
            }

            String suspensionNo = "SP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            ProcessSuspension suspension = new ProcessSuspension();
            suspension.setSuspensionNo(suspensionNo);
            suspension.setScheduleId(schedule.getId());
            suspension.setWorkstationId(schedule.getWorkstationId());
            suspension.setSuspensionType("FIRST_ARTICLE_FAIL");
            suspension.setSuspensionReason("首件检查不合格：" + (inspection.getDefectDesc() != null ? inspection.getDefectDesc() : ""));
            suspension.setSuspensionStatus("ACTIVE");
            suspension.setReporterId(inspectorId);
            suspension.setReportTime(LocalDateTime.now());
            suspension.setAffectNextProcess(1);
            processSuspensionMapper.insert(suspension);

            if (StringUtils.isNotBlank(schedule.getWeldSeamNo())) {
                weldSeamReportMapper.lockByWeldSeamNo(
                        schedule.getWeldSeamNo(),
                        "首件检查不合格，焊缝编号" + schedule.getWeldSeamNo() + "报工入口已锁定"
                );
            }
        } else {
            schedule.setScheduleStatus("WORKING");
            workScheduleMapper.updateById(schedule);

            Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
            if (ws != null) {
                ws.setCurrentStatus("WORKING");
                workstationMapper.updateById(ws);
            }
        }

        return inspectionNo;
    }

    @Override
    @Transactional
    public void recheck(Long inspectionId, Long recheckerId, Integer recheckResult) {
        FirstArticleInspection inspection = this.getById(inspectionId);
        if (inspection == null) {
            throw new BusinessException("首检记录不存在");
        }
        if (inspection.getRecheckRequired() == null || inspection.getRecheckRequired() == 0) {
            throw new BusinessException("该首检无需复检");
        }
        inspection.setRecheckResult(recheckResult);
        inspection.setRecheckTime(LocalDateTime.now());
        inspection.setRecheckerId(recheckerId);
        this.updateById(inspection);

        if (recheckResult == 1) {
            WorkSchedule schedule = workScheduleMapper.selectById(inspection.getScheduleId());
            if (schedule != null && "SUSPENDED".equals(schedule.getScheduleStatus())) {
                schedule.setScheduleStatus("WORKING");
                schedule.setResumeTime(LocalDateTime.now());
                workScheduleMapper.updateById(schedule);

                Workstation ws = workstationMapper.selectById(schedule.getWorkstationId());
                if (ws != null) {
                    ws.setCurrentStatus("WORKING");
                    workstationMapper.updateById(ws);
                }
            }

            ProcessSuspension suspension = processSuspensionMapper.selectOne(
                    new LambdaQueryWrapper<ProcessSuspension>()
                            .eq(ProcessSuspension::getScheduleId, inspection.getScheduleId())
                            .eq(ProcessSuspension::getSuspensionStatus, "ACTIVE")
                            .eq(ProcessSuspension::getSuspensionType, "FIRST_ARTICLE_FAIL")
                            .orderByDesc(ProcessSuspension::getCreateTime)
                            .last("LIMIT 1")
            );
            if (suspension != null) {
                suspension.setSuspensionStatus("RESOLVED");
                suspension.setResolverId(recheckerId);
                suspension.setResolveTime(LocalDateTime.now());
                suspension.setResolveMeasure("复检合格，解除暂停");
                processSuspensionMapper.updateById(suspension);
            }

            if (schedule != null && StringUtils.isNotBlank(schedule.getWeldSeamNo())) {
                weldSeamReportMapper.unlockByWeldSeamNo(schedule.getWeldSeamNo());
            }
        }
    }

    @Override
    public Map<String, Object> getRecheckDetail(Long inspectionId) {
        Map<String, Object> detail = new HashMap<>();
        FirstArticleInspection inspection = this.getById(inspectionId);
        if (inspection == null) {
            return detail;
        }
        detail.put("inspection", inspection);

        WorkSchedule schedule = workScheduleMapper.selectById(inspection.getScheduleId());
        if (schedule != null) {
            detail.put("machineNo", schedule.getMachineNo());
            detail.put("materialBatch", schedule.getMaterialBatch());
            detail.put("repairReason", schedule.getRepairReason());
            detail.put("weldSeamNo", schedule.getWeldSeamNo());

            if (StringUtils.isNotBlank(schedule.getWeldSeamNo())) {
                List<WeldSeamReport> reports = weldSeamReportMapper.selectByWeldSeamNo(schedule.getWeldSeamNo());
                detail.put("weldSeamReports", reports);
                long lockedCount = reports.stream().filter(r -> "LOCKED".equals(r.getWeldStatus())).count();
                detail.put("weldSeamLocked", lockedCount > 0);
            }
        }

        return detail;
    }
}
