package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("process_suspension")
public class ProcessSuspension {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String suspensionNo;

    private Long scheduleId;

    private Long workstationId;

    private String suspensionType;

    private String suspensionReason;

    private String suspensionStatus;

    private Long reporterId;

    private LocalDateTime reportTime;

    private Long resolverId;

    private LocalDateTime resolveTime;

    private String resolveMeasure;

    private Integer affectNextProcess;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
