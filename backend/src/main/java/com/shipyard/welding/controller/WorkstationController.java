package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.Workstation;
import com.shipyard.welding.service.WorkstationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workstation")
public class WorkstationController {

    @Autowired
    private WorkstationService workstationService;

    @PostMapping("/page")
    public Result<IPage<Workstation>> page(@RequestBody Map<String, Object> params) {
        return Result.success(workstationService.queryPage(params));
    }

    @GetMapping("/list")
    public Result<List<Workstation>> list() {
        return Result.success(workstationService.list());
    }

    @GetMapping("/{id}")
    public Result<Workstation> getById(@PathVariable Long id) {
        return Result.success(workstationService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Workstation workstation) {
        if (workstation.getCurrentStatus() == null) {
            workstation.setCurrentStatus("IDLE");
        }
        workstationService.save(workstation);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Workstation workstation) {
        workstationService.updateById(workstation);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workstationService.removeById(id);
        return Result.success();
    }
}
