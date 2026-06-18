package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("process_confirm")
public class ProcessConfirm {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;

    private Long welderId;

    private Long processCardId;

    private String confirmStatus;

    private String rejectReason;

    private LocalDateTime confirmTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
