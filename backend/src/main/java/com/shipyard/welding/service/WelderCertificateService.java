package com.shipyard.welding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shipyard.welding.entity.WelderCertificate;

import java.util.List;
import java.util.Map;

public interface WelderCertificateService extends IService<WelderCertificate> {

    IPage<WelderCertificate> queryPage(Map<String, Object> params);

    List<WelderCertificate> getByWelderId(Long welderId);

    void refreshCertStatus();
}
