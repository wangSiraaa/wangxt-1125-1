package com.shipyard.welding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shipyard.welding.entity.ProcessConfirm;
import com.shipyard.welding.entity.SafetyCheck;
import com.shipyard.welding.entity.WorkSchedule;

import java.util.List;
import java.util.Map;

public interface WorkScheduleService extends IService<WorkSchedule> {

    IPage<WorkSchedule> queryPage(Map<String, Object> params);

    String createSchedule(WorkSchedule schedule, Long teamLeaderId);

    void startWork(Long scheduleId, Long welderId);

    void confirmProcessCard(ProcessConfirm confirm);

    void submitFirstInspection(Long scheduleId);

    void completeWork(Long scheduleId);

    void suspendWork(Long scheduleId, String reason);

    void resumeWork(Long scheduleId);

    void cancelSchedule(Long scheduleId);

    List<SafetyCheck> getSafetyChecks(Long scheduleId);

    void saveSafetyChecks(List<SafetyCheck> checks);

    Map<String, Object> getStartPermitStatus(Long scheduleId);
}
