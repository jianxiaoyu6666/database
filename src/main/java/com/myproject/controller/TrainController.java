package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.entity.SeatInfo;
import com.myproject.entity.TrainSearchInfo;
import com.myproject.service.TrainService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trains")
public class TrainController {
    @Autowired
    private TrainService trainService;

    @PostConstruct
    public void init() {
        log.info(">>> TrainController 注册成功！监听 /api/trains/*");
    }


    /**
     * 查车次 — 输入出发站+到达站+日期，返回可乘坐的车次列表
     *
     * 前端对接说明：
     * 1. fromStationId / toStationId 必须是从 /api/stations/search 返回的 station.id，不是站名！
     * 2. date 格式必须是 yyyy-MM-dd，如 2026-07-01
     * 3. trainType 可选，支持 G/D/T/K/Z/C，不传相当于查询全部类型
     * 4. 返回的 availableSeats 是剩余票数，前端展示用
     *
     * 典型调用：GET /api/trains/search?fromStationId=10&toStationId=55&date=2026-07-01
     */
    @GetMapping("/search")
    public Result searchTrains(@RequestParam Long fromStationId,
                               @RequestParam Long toStationId,
                               @RequestParam String date,
                               @RequestParam(required = false) String trainType){
        List<TrainSearchInfo> trainSearchInfo = trainService.search(fromStationId,toStationId,date,trainType);
        return Result.success(trainSearchInfo);
    }

    /**
     * 查某个车次的经停站，train_route表无需对应实体类，作为只读表，它只需要可以被访问到数据就可以了（数据可以直接自己捏造，因为也不可能就得到真正的火车实况，本项目是模拟的）
     * 这个在买票的时候用的到
     */
//    @GetMapping("/searchRoute")
//    public Result searchRoute(@RequestParam ){
//
//    }

    /**
     * 查可用座位 — 选了某个车次后，查看还有哪些座位可以买
     *
     * 前端对接说明：
     * 1. scheduleId 从查车次结果的 scheduleId 字段拿
     * 2. fromStationId / toStationId 和查车次时传的一样
     * 3. 返回每个座位：seatId(购票时用)、seatNumber(01A展示)、carriageType(二等座展示)
     *
     * 典型调用：GET /api/trains/6/seats?fromStationId=1&toStationId=3
     */
    @GetMapping("/{scheduleId}/seats")
    public Result searchSeats(@PathVariable Long scheduleId,
                              @RequestParam Long fromStationId,
                              @RequestParam Long toStationId){
        return Result.success(trainService.findSeats(scheduleId, fromStationId, toStationId));
    }
}
