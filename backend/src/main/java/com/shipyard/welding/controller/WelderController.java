package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.Welder;
import com.shipyard.welding.entity.WelderCertificate;
import com.shipyard.welding.service.WelderCertificateService;
import com.shipyard.welding.service.WelderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/welder")
public class WelderController {

    @Autowired
    private WelderService welderService;

    @Autowired
    private WelderCertificateService certificateService;

    @PostMapping("/page")
    public Result<IPage<Welder>> page(@RequestBody Map<String, Object> params) {
        return Result.success(welderService.queryPage(params));
    }

    @GetMapping("/list")
    public Result<List<Welder>> list() {
        return Result.success(welderService.list());
    }

    @GetMapping("/{id}")
    public Result<Welder> getById(@PathVariable Long id) {
        return Result.success(welderService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Welder welder) {
        welderService.save(welder);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Welder welder) {
        welderService.updateById(welder);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        welderService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}/certValid")
    public Result<Boolean> checkCertValid(@PathVariable Long id) {
        return Result.success(welderService.checkWelderCertValid(id));
    }

    @GetMapping("/{welderId}/certificates")
    public Result<List<WelderCertificate>> getCertificates(@PathVariable Long welderId) {
        return Result.success(certificateService.getByWelderId(welderId));
    }

    @PostMapping("/certificate/page")
    public Result<IPage<WelderCertificate>> certPage(@RequestBody Map<String, Object> params) {
        return Result.success(certificateService.queryPage(params));
    }

    @PostMapping("/certificate")
    public Result<Void> saveCert(@RequestBody WelderCertificate cert) {
        certificateService.save(cert);
        certificateService.refreshCertStatus();
        return Result.success();
    }

    @PutMapping("/certificate")
    public Result<Void> updateCert(@RequestBody WelderCertificate cert) {
        certificateService.updateById(cert);
        certificateService.refreshCertStatus();
        return Result.success();
    }

    @DeleteMapping("/certificate/{id}")
    public Result<Void> deleteCert(@PathVariable Long id) {
        certificateService.removeById(id);
        return Result.success();
    }

    @PostMapping("/certificate/refreshStatus")
    public Result<Void> refreshCertStatus() {
        certificateService.refreshCertStatus();
        return Result.success();
    }
}
