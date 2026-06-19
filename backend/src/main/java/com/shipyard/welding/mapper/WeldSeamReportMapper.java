package com.shipyard.welding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shipyard.welding.entity.WeldSeamReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WeldSeamReportMapper extends BaseMapper<WeldSeamReport> {

    @Select("SELECT * FROM weld_seam_report WHERE weld_seam_no = #{weldSeamNo}")
    List<WeldSeamReport> selectByWeldSeamNo(@Param("weldSeamNo") String weldSeamNo);

    @Update("UPDATE weld_seam_report SET weld_status = 'LOCKED', lock_reason = #{lockReason}, lock_time = NOW() WHERE weld_seam_no = #{weldSeamNo} AND weld_status IN ('IN_PROGRESS', 'COMPLETED')")
    int lockByWeldSeamNo(@Param("weldSeamNo") String weldSeamNo, @Param("lockReason") String lockReason);

    @Update("UPDATE weld_seam_report SET weld_status = 'IN_PROGRESS', unlock_time = NOW(), lock_reason = NULL WHERE weld_seam_no = #{weldSeamNo} AND weld_status = 'LOCKED'")
    int unlockByWeldSeamNo(@Param("weldSeamNo") String weldSeamNo);

    @Select("SELECT COUNT(*) FROM weld_seam_report WHERE weld_seam_no = #{weldSeamNo} AND weld_status = 'LOCKED'")
    int countLockedByWeldSeamNo(@Param("weldSeamNo") String weldSeamNo);
}
