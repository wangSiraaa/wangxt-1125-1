package com.shipyard.welding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shipyard.welding.entity.ProcessCard;

import java.util.Map;

public interface ProcessCardService extends IService<ProcessCard> {

    IPage<ProcessCard> queryPage(Map<String, Object> params);

    boolean issueCard(Long id, Long issuerId);

    boolean approveCard(Long id);
}
