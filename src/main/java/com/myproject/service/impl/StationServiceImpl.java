package com.myproject.service.impl;

import com.myproject.entity.station;
import com.myproject.mapper.StationMapper;
import com.myproject.service.StationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class StationServiceImpl implements StationService {
    @Autowired
    private StationMapper stationMapper;

    @PostConstruct
    public void init() {
        log.info(">>> StationServiceImpl 初始化成功！stationMapper={}", stationMapper != null ? "已注入" : "未注入!");
    }

    /**
     * 前端对接说明：
     * 1. 调用 GET /api/stations/search?keyword=xxx 获取车站列表
     * 2. 返回的 JSON 中 data 数组每个元素包含 id、stationName、city 等字段
     * 3. 下拉菜单展示 stationName，选中后保存该车站的 id
     * 4. 查车次时用 id 作为 fromStationId / toStationId 参数
     *    不要传站名字符串！后端存储过程只认 ID
     */
    @Override
    public List<station> search(String keyword) {
        log.info(">>> StationServiceImpl.search() keyword={}", keyword);
        List<station> l = stationMapper.searchByKeyWord(keyword);
        log.info(">>> 数据库查询完成，结果数: {}", l.size());
        return l;
    }
}
