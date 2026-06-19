package com.shipyard.welding.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shipyard.welding.entity.FirstArticleInspection;

import java.util.Map;

public interface FirstArticleInspectionService extends IService<FirstArticleInspection> {

    IPage<FirstArticleInspection> queryPage(Map<String, Object> params);

    FirstArticleInspection getByScheduleId(Long scheduleId);

    String saveInspection(FirstArticleInspection inspection, Long inspectorId);

    void recheck(Long inspectionId, Long recheckerId, Integer recheckResult);

    Map<String, Object> getRecheckDetail(Long inspectionId);
}
