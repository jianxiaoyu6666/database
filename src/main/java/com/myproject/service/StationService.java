package com.myproject.service;

import com.myproject.entity.station;

import java.util.List;

public interface StationService {

    /**
     * 车站模糊搜索
     * 返回的车站对象包含 id 和 stationName，前端展示 stationName，传 id 给查车次接口
     */
    List<station> search(String keyword);
}
