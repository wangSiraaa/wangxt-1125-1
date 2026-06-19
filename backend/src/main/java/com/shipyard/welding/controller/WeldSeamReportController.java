package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.WeldSeamReport;
import com.shipyard.welding.mapper.WeldSeamReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/weldSeamReport")
public class WeldSeamReportController {

    @Autowired
    private WeldSeamReportMapper weldSeamReportMapper;

    @GetMapping("/list")
    public Result<List<WeldSeamReport>> list() {
        return Result.success(weldSeamReportMapper.selectList(
                new LambdaQueryWrapper<WeldSeamReport>().orderByDesc(WeldSeamReport::getCreateTime)
        ));
    }

    @GetMapping("/{id}")
    public Result<WeldSeamReport> getById(@PathVariable Long id) {
        return Result.success(weldSeamReportMapper.selectById(id));
    }

    @GetMapping("/seam/{weldSeamNo}")
    public Result<List<WeldSeamReport>> getByWeldSeamNo(@PathVariable String weldSeamNo) {
        return Result.success(weldSeamReportMapper.selectByWeldSeamNo(weldSeamNo));
    }

    @GetMapping("/schedule/{scheduleId}")
    public Result<List<WeldSeamReport>> getBySchedule(@PathVariable Long scheduleId) {
        return Result.success(weldSeamReportMapper.selectList(
                new LambdaQueryWrapper<WeldSeamReport>()
                        .eq(WeldSeamReport::getScheduleId, scheduleId)
                        .orderByDesc(WeldSeamReport::getCreateTime)
        ));
    }

    @PostMapping
    public Result<String> save(@RequestBody WeldSeamReport report) {
        report.setReportNo("WR" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        weldSeamReportMapper.insert(report);
        return Result.success(report.getReportNo());
    }

    @PutMapping
    public Result<Void> update(@RequestBody WeldSeamReport report) {
        weldSeamReportMapper.updateById(report);
        return Result.success();
    }

    @PostMapping("/lock/{weldSeamNo}")
    public Result<Integer> lockByWeldSeamNo(@PathVariable String weldSeamNo, @RequestParam String lockReason) {
        int rows = weldSeamReportMapper.lockByWeldSeamNo(weldSeamNo, lockReason);
        return Result.success(rows);
    }

    @PostMapping("/unlock/{weldSeamNo}")
    public Result<Integer> unlockByWeldSeamNo(@PathVariable String weldSeamNo) {
        int rows = weldSeamReportMapper.unlockByWeldSeamNo(weldSeamNo);
        return Result.success(rows);
    }

    @GetMapping("/locked/{weldSeamNo}")
    public Result<Map<String, Object>> checkLocked(@PathVariable String weldSeamNo) {
        int count = weldSeamReportMapper.countLockedByWeldSeamNo(weldSeamNo);
        List<WeldSeamReport> reports = weldSeamReportMapper.selectByWeldSeamNo(weldSeamNo);
        return Result.success(Map.of("locked", count > 0, "reports", reports));
    }
}
