package com.shipyard.welding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shipyard.welding.entity.WelderCertificate;
import com.shipyard.welding.mapper.WelderCertificateMapper;
import com.shipyard.welding.service.WelderCertificateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class WelderCertificateServiceImpl extends ServiceImpl<WelderCertificateMapper, WelderCertificate> implements WelderCertificateService {

    @Override
    public IPage<WelderCertificate> queryPage(Map<String, Object> params) {
        Page<WelderCertificate> page = new Page<>(
                Long.parseLong(params.getOrDefault("pageNum", "1").toString()),
                Long.parseLong(params.getOrDefault("pageSize", "10").toString())
        );
        LambdaQueryWrapper<WelderCertificate> wrapper = new LambdaQueryWrapper<>();
        if (params.get("welderId") != null) {
            wrapper.eq(WelderCertificate::getWelderId, params.get("welderId"));
        }
        String keyword = (String) params.get("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(WelderCertificate::getCertNo, keyword)
                    .or().like(WelderCertificate::getCertType, keyword));
        }
        if (params.get("certStatus") != null) {
            wrapper.eq(WelderCertificate::getCertStatus, params.get("certStatus"));
        }
        wrapper.orderByDesc(WelderCertificate::getExpiryDate);
        return this.page(page, wrapper);
    }

    @Override
    public List<WelderCertificate> getByWelderId(Long welderId) {
        return baseMapper.selectByWelderId(welderId);
    }

    @Override
    public void refreshCertStatus() {
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusMonths(3);
        List<WelderCertificate> list = this.list();
        for (WelderCertificate cert : list) {
            if (cert.getCertStatus() == 0) continue;
            LocalDate expiry = cert.getExpiryDate();
            if (expiry.isBefore(today)) {
                cert.setCertStatus(3);
            } else if (expiry.isBefore(warningDate)) {
                cert.setCertStatus(2);
            } else {
                cert.setCertStatus(1);
            }
            this.updateById(cert);
        }
    }
}
