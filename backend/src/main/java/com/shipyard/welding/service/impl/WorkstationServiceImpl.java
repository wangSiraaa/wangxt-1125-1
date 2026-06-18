package com.shipyard.welding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shipyard.welding.entity.Workstation;
import com.shipyard.welding.exception.BusinessException;
import com.shipyard.welding.mapper.WorkstationMapper;
import com.shipyard.welding.service.WorkstationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WorkstationServiceImpl extends ServiceImpl<WorkstationMapper, Workstation> implements WorkstationService {

    @Override
    public IPage<Workstation> queryPage(Map<String, Object> params) {
        Page<Workstation> page = new Page<>(
                Long.parseLong(params.getOrDefault("pageNum", "1").toString()),
                Long.parseLong(params.getOrDefault("pageSize", "10").toString())
        );
        LambdaQueryWrapper<Workstation> wrapper = new LambdaQueryWrapper<>();
        String keyword = (String) params.get("keyword");
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Workstation::getStationNo, keyword)
                    .or().like(Workstation::getStationName, keyword));
        }
        if (params.get("currentStatus") != null && StringUtils.isNotBlank(params.get("currentStatus").toString())) {
            wrapper.eq(Workstation::getCurrentStatus, params.get("currentStatus"));
        }
        if (params.get("status") != null) {
            wrapper.eq(Workstation::getStatus, params.get("status"));
        }
        wrapper.orderByAsc(Workstation::getStationNo);
        return this.page(page, wrapper);
    }

    @Override
    public void updateStatus(Long id, String status) {
        Workstation ws = this.getById(id);
        if (ws == null) {
            throw new BusinessException("工位不存在");
        }
        ws.setCurrentStatus(status);
        this.updateById(ws);
    }
}
