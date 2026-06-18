package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("safety_check")
public class SafetyCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scheduleId;

    private Long workstationId;

    private String checkType;

    private String checkItem;

    private Integer checkResult;

    private String checkDesc;

    private Long checkerId;

    private LocalDateTime checkTime;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
