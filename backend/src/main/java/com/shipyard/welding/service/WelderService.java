package com.shipyard.welding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shipyard.welding.entity.Welder;

import java.util.Map;

public interface WelderService extends IService<Welder> {

    IPage<Welder> queryPage(Map<String, Object> params);

    boolean checkWelderCertValid(Long welderId);
}
