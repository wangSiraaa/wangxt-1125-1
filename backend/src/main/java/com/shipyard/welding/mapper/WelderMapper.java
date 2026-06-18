package com.shipyard.welding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shipyard.welding.entity.Welder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WelderMapper extends BaseMapper<Welder> {
}
