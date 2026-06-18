package com.shipyard.welding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("workstation")
public class Workstation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stationNo;

    private String stationName;

    private String area;

    private String equipment;

    private String currentStatus;

    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
