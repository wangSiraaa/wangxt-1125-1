package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("weld_seam_report")
public class WeldSeamReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportNo;

    private Long scheduleId;

    private String weldSeamNo;

    private Long workstationId;

    private Long welderId;

    private Long processCardId;

    private String weldPosition;

    private BigDecimal weldLength;

    private String weldStatus;

    private String lockReason;

    private LocalDateTime lockTime;

    private LocalDateTime unlockTime;

    private String machineNo;

    private String materialBatch;

    private String fillerMaterial;

    private LocalDateTime workStartTime;

    private LocalDateTime workEndTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
