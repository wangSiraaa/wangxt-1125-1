package com.shipyard.welding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shipyard.welding.entity.Welder;
import com.shipyard.welding.mapper.WelderMapper;
import com.shipyard.welding.mapper.WelderCertificateMapper;
import com.shipyard.welding.service.WelderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
public class WelderServiceImpl extends ServiceImpl<WelderMapper, Welder> implements WelderService {

    @Autowired
    private WelderCertificateMapper welderCertificateMapper;

    @Override
    public IPage<Welder> queryPage(Map<String, Object> params) {
        Page<Welder> page = new Page<>(
                Long.parseLong(params.getOrDefault("pageNum", "1").toString()),
                Long.parseLong(params.getOrDefault("pageSize", "10").toString())
        );
        LambdaQueryWrapper<Welder> wrapper = new LambdaQueryWrapper<>();
        String keyword = (String) params.get("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Welder::getWelderNo, keyword)
                    .or().like(Welder::getWelderName, keyword));
        }
        if (params.get("status") != null) {
            wrapper.eq(Welder::getStatus, params.get("status"));
        }
        wrapper.orderByDesc(Welder::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public boolean checkWelderCertValid(Long welderId) {
        int count = welderCertificateMapper.countValidCerts(welderId, LocalDate.now());
        return count > 0;
    }
}
