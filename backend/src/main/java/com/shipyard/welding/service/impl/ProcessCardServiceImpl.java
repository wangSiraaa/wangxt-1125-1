package com.shipyard.welding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shipyard.welding.entity.ProcessCard;
import com.shipyard.welding.exception.BusinessException;
import com.shipyard.welding.mapper.ProcessCardMapper;
import com.shipyard.welding.service.ProcessCardService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ProcessCardServiceImpl extends ServiceImpl<ProcessCardMapper, ProcessCard> implements ProcessCardService {

    @Override
    public IPage<ProcessCard> queryPage(Map<String, Object> params) {
        Page<ProcessCard> page = new Page<>(
                Long.parseLong(params.getOrDefault("pageNum", "1").toString()),
                Long.parseLong(params.getOrDefault("pageSize", "10").toString())
        );
        LambdaQueryWrapper<ProcessCard> wrapper = new LambdaQueryWrapper<>();
        String keyword = (String) params.get("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ProcessCard::getCardNo, keyword)
                    .or().like(ProcessCard::getCardName, keyword)
                    .or().like(ProcessCard::getProjectName, keyword));
        }
        if (params.get("cardStatus") != null && StringUtils.isNotBlank(params.get("cardStatus").toString())) {
            wrapper.eq(ProcessCard::getCardStatus, params.get("cardStatus"));
        }
        wrapper.orderByDesc(ProcessCard::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public boolean issueCard(Long id, Long issuerId) {
        ProcessCard card = this.getById(id);
        if (card == null) {
            throw new BusinessException("工艺卡不存在");
        }
        if (!"APPROVED".equals(card.getCardStatus()) && !"DRAFT".equals(card.getCardStatus())) {
            throw new BusinessException("只有草稿或已审批状态的工艺卡才能下发");
        }
        card.setCardStatus("ISSUED");
        card.setIssuerId(issuerId);
        card.setIssueTime(LocalDateTime.now());
        return this.updateById(card);
    }

    @Override
    public boolean approveCard(Long id) {
        ProcessCard card = this.getById(id);
        if (card == null) {
            throw new BusinessException("工艺卡不存在");
        }
        if (!"DRAFT".equals(card.getCardStatus())) {
            throw new BusinessException("只有草稿状态的工艺卡才能审批");
        }
        card.setCardStatus("APPROVED");
        return this.updateById(card);
    }
}
