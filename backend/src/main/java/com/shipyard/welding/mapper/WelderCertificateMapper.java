package com.shipyard.welding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shipyard.welding.entity.WelderCertificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WelderCertificateMapper extends BaseMapper<WelderCertificate> {

    @Select("SELECT * FROM welder_certificate WHERE welder_id = #{welderId} ORDER BY expiry_date DESC")
    List<WelderCertificate> selectByWelderId(@Param("welderId") Long welderId);

    @Select("SELECT COUNT(*) FROM welder_certificate WHERE welder_id = #{welderId} AND cert_status = 1 AND expiry_date >= #{today}")
    int countValidCerts(@Param("welderId") Long welderId, @Param("today") LocalDate today);
}
