package com.shipyard.welding.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shipyard.welding.common.Result;
import com.shipyard.welding.entity.ProcessCard;
import com.shipyard.welding.service.ProcessCardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/processCard")
public class ProcessCardController {

    @Autowired
    private ProcessCardService processCardService;

    @PostMapping("/page")
    public Result<IPage<ProcessCard>> page(@RequestBody Map<String, Object> params) {
        return Result.success(processCardService.queryPage(params));
    }

    @GetMapping("/list")
    public Result<List<ProcessCard>> list() {
        return Result.success(processCardService.list());
    }

    @GetMapping("/issued")
    public Result<List<ProcessCard>> issuedList() {
        return Result.success(processCardService.lambdaQuery()
                .eq(ProcessCard::getCardStatus, "ISSUED")
                .orderByDesc(ProcessCard::getCreateTime)
                .list());
    }

    @GetMapping("/{id}")
    public Result<ProcessCard> getById(@PathVariable Long id) {
        return Result.success(processCardService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ProcessCard card) {
        if (card.getCardStatus() == null) {
            card.setCardStatus("DRAFT");
        }
        processCardService.save(card);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody ProcessCard card) {
        processCardService.updateById(card);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        processCardService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        processCardService.approveCard(id);
        return Result.success();
    }

    @PostMapping("/{id}/issue")
    public Result<Void> issue(@PathVariable Long id, @RequestParam Long issuerId) {
        processCardService.issueCard(id, issuerId);
        return Result.success();
    }
}
