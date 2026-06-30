package com.myproject.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class station {
    private Long id;
    private String stationName;
    private String stationCode;//车站代码(拼音缩写)
    private String city;
    private String province;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
