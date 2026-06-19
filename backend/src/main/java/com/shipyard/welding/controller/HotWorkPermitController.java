package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.HotWorkPermit;
import com.shipyard.welding.mapper.HotWorkPermitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotwork")
public class HotWorkPermitController {

    @Autowired
    private HotWorkPermitMapper hotWorkPermitMapper;

    @GetMapping("/list")
    public Result<List<HotWorkPermit>> list() {
        return Result.success(hotWorkPermitMapper.selectList(
                new LambdaQueryWrapper<HotWorkPermit>().orderByDesc(HotWorkPermit::getCreateTime)
        ));
    }

    @GetMapping("/{id}")
    public Result<HotWorkPermit> getById(@PathVariable Long id) {
        return Result.success(hotWorkPermitMapper.selectById(id));
    }

    @GetMapping("/workstation/{workstationId}")
    public Result<List<HotWorkPermit>> getByWorkstation(@PathVariable Long workstationId) {
        return Result.success(hotWorkPermitMapper.selectList(
                new LambdaQueryWrapper<HotWorkPermit>()
                        .eq(HotWorkPermit::getWorkstationId, workstationId)
                        .orderByDesc(HotWorkPermit::getCreateTime)
        ));
    }

    @PostMapping
    public Result<String> save(@RequestBody HotWorkPermit permit, @RequestParam Long applicantId) {
        permit.setPermitNo("HW" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        permit.setApplicantId(applicantId);
        permit.setApplyTime(LocalDateTime.now());
        hotWorkPermitMapper.insert(permit);
        return Result.success(permit.getPermitNo());
    }

    @PutMapping
    public Result<Void> update(@RequestBody HotWorkPermit permit) {
        hotWorkPermitMapper.updateById(permit);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestParam Long approverId) {
        HotWorkPermit permit = hotWorkPermitMapper.selectById(id);
        if (permit == null) return Result.success();
        permit.setPermitStatus("APPROVED");
        permit.setApproverId(approverId);
        permit.setApproveTime(LocalDateTime.now());
        hotWorkPermitMapper.updateById(permit);
        return Result.success();
    }

    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam Long approverId) {
        HotWorkPermit permit = hotWorkPermitMapper.selectById(id);
        if (permit == null) return Result.success();
        permit.setPermitStatus("REJECTED");
        permit.setApproverId(approverId);
        permit.setApproveTime(LocalDateTime.now());
        hotWorkPermitMapper.updateById(permit);
        return Result.success();
    }
}
