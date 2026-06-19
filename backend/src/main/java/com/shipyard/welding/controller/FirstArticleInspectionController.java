package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.FirstArticleInspection;
import com.shipyard.welding.entity.ProcessSuspension;
import com.shipyard.welding.mapper.ProcessSuspensionMapper;
import com.shipyard.welding.service.FirstArticleInspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inspection")
public class FirstArticleInspectionController {

    @Autowired
    private FirstArticleInspectionService inspectionService;

    @Autowired
    private ProcessSuspensionMapper suspensionMapper;

    @PostMapping("/page")
    public Result<IPage<FirstArticleInspection>> page(@RequestBody Map<String, Object> params) {
        return Result.success(inspectionService.queryPage(params));
    }

    @GetMapping("/schedule/{scheduleId}")
    public Result<FirstArticleInspection> getByScheduleId(@PathVariable Long scheduleId) {
        return Result.success(inspectionService.getByScheduleId(scheduleId));
    }

    @GetMapping("/{id}")
    public Result<FirstArticleInspection> getById(@PathVariable Long id) {
        return Result.success(inspectionService.getById(id));
    }

    @PostMapping
    public Result<String> save(@RequestBody FirstArticleInspection inspection, @RequestParam Long inspectorId) {
        return Result.success(inspectionService.saveInspection(inspection, inspectorId));
    }

    @PutMapping
    public Result<Void> update(@RequestBody FirstArticleInspection inspection) {
        inspectionService.updateById(inspection);
        return Result.success();
    }

    @PostMapping("/{id}/recheck")
    public Result<Void> recheck(@PathVariable Long id, @RequestParam Long recheckerId, @RequestParam Integer recheckResult) {
        inspectionService.recheck(id, recheckerId, recheckResult);
        return Result.success();
    }

    @GetMapping("/{id}/recheckDetail")
    public Result<Map<String, Object>> getRecheckDetail(@PathVariable Long id) {
        return Result.success(inspectionService.getRecheckDetail(id));
    }

    @GetMapping("/suspension/list")
    public Result<List<ProcessSuspension>> suspensionList() {
        return Result.success(suspensionMapper.selectList(
                new LambdaQueryWrapper<ProcessSuspension>().orderByDesc(ProcessSuspension::getCreateTime)
        ));
    }

    @GetMapping("/suspension/schedule/{scheduleId}")
    public Result<List<ProcessSuspension>> getSuspensionsBySchedule(@PathVariable Long scheduleId) {
        return Result.success(suspensionMapper.selectList(
                new LambdaQueryWrapper<ProcessSuspension>()
                        .eq(ProcessSuspension::getScheduleId, scheduleId)
                        .orderByDesc(ProcessSuspension::getCreateTime)
        ));
    }

    @PostMapping("/suspension/resolve/{id}")
    public Result<Void> resolveSuspension(@PathVariable Long id, @RequestParam Long resolverId, @RequestParam String resolveMeasure) {
        ProcessSuspension suspension = suspensionMapper.selectById(id);
        if (suspension != null) {
            suspension.setSuspensionStatus("RESOLVED");
            suspension.setResolverId(resolverId);
            suspension.setResolveTime(LocalDateTime.now());
            suspension.setResolveMeasure(resolveMeasure);
            suspensionMapper.updateById(suspension);
        }
        return Result.success();
    }
}
