package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.ProcessDeviation;
import com.shipyard.welding.mapper.ProcessDeviationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/deviation")
public class ProcessDeviationController {

    @Autowired
    private ProcessDeviationMapper processDeviationMapper;

    @GetMapping("/list")
    public Result<List<ProcessDeviation>> list() {
        return Result.success(processDeviationMapper.selectList(
                new LambdaQueryWrapper<ProcessDeviation>().orderByDesc(ProcessDeviation::getCreateTime)
        ));
    }

    @GetMapping("/{id}")
    public Result<ProcessDeviation> getById(@PathVariable Long id) {
        return Result.success(processDeviationMapper.selectById(id));
    }

    @GetMapping("/processCard/{processCardId}")
    public Result<List<ProcessDeviation>> getByProcessCard(@PathVariable Long processCardId) {
        return Result.success(processDeviationMapper.selectList(
                new LambdaQueryWrapper<ProcessDeviation>()
                        .eq(ProcessDeviation::getProcessCardId, processCardId)
                        .orderByDesc(ProcessDeviation::getCreateTime)
        ));
    }

    @PostMapping
    public Result<String> save(@RequestBody ProcessDeviation deviation, @RequestParam Long applicantId) {
        deviation.setDeviationNo("DV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        deviation.setApplicantId(applicantId);
        deviation.setApplyTime(LocalDateTime.now());
        processDeviationMapper.insert(deviation);
        return Result.success(deviation.getDeviationNo());
    }

    @PutMapping
    public Result<Void> update(@RequestBody ProcessDeviation deviation) {
        processDeviationMapper.updateById(deviation);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestParam Long reviewerId) {
        ProcessDeviation deviation = processDeviationMapper.selectById(id);
        if (deviation == null) return Result.success();
        deviation.setDeviationStatus("APPROVED");
        deviation.setReviewerId(reviewerId);
        deviation.setReviewTime(LocalDateTime.now());
        processDeviationMapper.updateById(deviation);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam Long reviewerId, @RequestParam String opinion) {
        ProcessDeviation deviation = processDeviationMapper.selectById(id);
        if (deviation == null) return Result.success();
        deviation.setDeviationStatus("REJECTED");
        deviation.setReviewerId(reviewerId);
        deviation.setReviewTime(LocalDateTime.now());
        deviation.setReviewOpinion(opinion);
        processDeviationMapper.updateById(deviation);
        return Result.success();
    }
}
