package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.entity.station;
import com.myproject.service.StationService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/stations")
public class StationController {
    @Autowired
    private StationService stationService;

    @PostConstruct
    public void init() {
        log.info(">>> StationController 注册成功！监听 /api/stations/*");
    }

    //查询车站，和查询车次配套使用，因为用户不可能输入一个车站ID作为查询信息
    /**
     * 车站模糊搜索 — 配合查车次接口使用
     *
     * 前端同学注意：
     * 1. 用户输入关键字 → 调此接口获取匹配的车站列表
     * 2. 把返回的 stationName 做成下拉菜单展示给用户
     * 3. 用户选中某个车站后，**不要传 stationName 给查车次接口**，要传 id
     * 4. 示例流程：用户输入"北京" → 下拉出现"北京南" → 选中 → 拿到 id=10
     *     调 GET /api/trains/search?fromStationId=10&toStationId=55&date=2026-07-01
     *
     * @param keyword 车站名称/城市名/拼音关键词
     * @return Result.data 是 List<station>，每个 station 包含 id、stationName、city 等
     */
    @GetMapping("/search")
    public Result search(@RequestParam String keyword){
        log.info(">>> StationController.search() 被调用！keyword={}", keyword);
        List<station> l = stationService.search(keyword);
        log.info(">>> 搜索完成，结果数: {}", l.size());
        return Result.success(l);
    }
}
