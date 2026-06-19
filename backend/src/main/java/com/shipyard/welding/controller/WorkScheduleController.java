package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.*;
import com.shipyard.welding.service.WorkScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedule")
public class WorkScheduleController {

    @Autowired
    private WorkScheduleService workScheduleService;

    @PostMapping("/page")
    public Result<IPage<WorkSchedule>> page(@RequestBody Map<String, Object> params) {
        return Result.success(workScheduleService.queryPage(params));
    }

    @GetMapping("/{id}")
    public Result<WorkSchedule> getById(@PathVariable Long id) {
        return Result.success(workScheduleService.getById(id));
    }

    @GetMapping("/{id}/detail")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return Result.success(workScheduleService.getScheduleDetail(id));
    }

    @PostMapping
    public Result<String> create(@RequestBody WorkSchedule schedule, @RequestParam Long teamLeaderId) {
        return Result.success(workScheduleService.createSchedule(schedule, teamLeaderId));
    }

    @PutMapping
    public Result<Void> update(@RequestBody WorkSchedule schedule) {
        workScheduleService.updateById(schedule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workScheduleService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/start")
    public Result<Void> startWork(@PathVariable Long id, @RequestParam Long welderId) {
        workScheduleService.startWork(id, welderId);
        return Result.success();
    }

    @PostMapping("/confirmProcess")
    public Result<Void> confirmProcessCard(@RequestBody ProcessConfirm confirm) {
        workScheduleService.confirmProcessCard(confirm);
        return Result.success();
    }

    @PostMapping("/{id}/submitInspection")
    public Result<Void> submitFirstInspection(@PathVariable Long id) {
        workScheduleService.submitFirstInspection(id);
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    public Result<Void> completeWork(@PathVariable Long id) {
        workScheduleService.completeWork(id);
        return Result.success();
    }

    @PostMapping("/{id}/suspend")
    public Result<Void> suspendWork(@PathVariable Long id, @RequestParam String reason) {
        workScheduleService.suspendWork(id, reason);
        return Result.success();
    }

    @PostMapping("/{id}/resume")
    public Result<Void> resumeWork(@PathVariable Long id) {
        workScheduleService.resumeWork(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancelSchedule(@PathVariable Long id) {
        workScheduleService.cancelSchedule(id);
        return Result.success();
    }

    @GetMapping("/{id}/safetyChecks")
    public Result<List<SafetyCheck>> getSafetyChecks(@PathVariable Long id) {
        return Result.success(workScheduleService.getSafetyChecks(id));
    }

    @PostMapping("/safetyChecks")
    public Result<Void> saveSafetyChecks(@RequestBody List<SafetyCheck> checks) {
        workScheduleService.saveSafetyChecks(checks);
        return Result.success();
    }

    @GetMapping("/{id}/permitStatus")
    public Result<Map<String, Object>> getStartPermitStatus(@PathVariable Long id) {
        return Result.success(workScheduleService.getStartPermitStatus(id));
    }
}
