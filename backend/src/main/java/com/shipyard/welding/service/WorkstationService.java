package com.shipyard.welding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shipyard.welding.entity.Workstation;

import java.util.Map;

public interface WorkstationService extends IService<Workstation> {

    IPage<Workstation> queryPage(Map<String, Object> params);

    void updateStatus(Long id, String status);
}
