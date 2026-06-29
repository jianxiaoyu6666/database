package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.service.WindowStatisticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics/window")
public class StatisticsController {
    private final WindowStatisticsService statisticsService;

    public StatisticsController(WindowStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/route-ranking")
    public Result routeRanking(@RequestParam(required = false) Integer limit) {
        return Result.success(statisticsService.routeRanking(limit));
    }

    @GetMapping("/user-ranking")
    public Result userRanking(@RequestParam(required = false) Integer limit) {
        return Result.success(statisticsService.userConsumptionRanking(limit));
    }

    @GetMapping("/train-trend")
    public Result trainTrend(@RequestParam(required = false) String trainNumber,
                             @RequestParam(required = false) Integer days) {
        return Result.success(statisticsService.trainTrend(trainNumber, days));
    }
}
