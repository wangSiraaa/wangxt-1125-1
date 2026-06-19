package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.SafetyReview;
import com.shipyard.welding.mapper.SafetyReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/safetyReview")
public class SafetyReviewController {

    @Autowired
    private SafetyReviewMapper safetyReviewMapper;

    @GetMapping("/pending")
    public Result<List<SafetyReview>> pendingList() {
        return Result.success(safetyReviewMapper.selectList(
                new LambdaQueryWrapper<SafetyReview>()
                        .eq(SafetyReview::getReviewStatus, "PENDING")
                        .orderByAsc(SafetyReview::getCreateTime)
        ));
    }

    @GetMapping("/list")
    public Result<List<SafetyReview>> list() {
        return Result.success(safetyReviewMapper.selectList(
                new LambdaQueryWrapper<SafetyReview>().orderByDesc(SafetyReview::getCreateTime)
        ));
    }

    @GetMapping("/{id}")
    public Result<SafetyReview> getById(@PathVariable Long id) {
        return Result.success(safetyReviewMapper.selectById(id));
    }

    @GetMapping("/schedule/{scheduleId}")
    public Result<SafetyReview> getBySchedule(@PathVariable Long scheduleId) {
        return Result.success(safetyReviewMapper.selectOne(
                new LambdaQueryWrapper<SafetyReview>()
                        .eq(SafetyReview::getScheduleId, scheduleId)
                        .orderByDesc(SafetyReview::getCreateTime)
                        .last("LIMIT 1")
        ));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestParam Long reviewerId,
                                @RequestParam(required = false) String safetyMeasures,
                                @RequestParam(required = false) String reviewOpinion) {
        SafetyReview review = safetyReviewMapper.selectById(id);
        if (review == null) return Result.success();
        review.setReviewStatus("APPROVED");
        review.setReviewerId(reviewerId);
        review.setReviewTime(LocalDateTime.now());
        review.setSafetyMeasures(safetyMeasures);
        review.setReviewOpinion(reviewOpinion);
        safetyReviewMapper.updateById(review);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam Long reviewerId,
                               @RequestParam String reviewOpinion) {
        SafetyReview review = safetyReviewMapper.selectById(id);
        if (review == null) return Result.success();
        review.setReviewStatus("REJECTED");
        review.setReviewerId(reviewerId);
        review.setReviewTime(LocalDateTime.now());
        review.setReviewOpinion(reviewOpinion);
        safetyReviewMapper.updateById(review);
        return Result.success();
    }
}
