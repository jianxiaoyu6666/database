package com.myproject.service;

import java.util.List;
import java.util.Map;

public interface WindowStatisticsService {
    List<Map<String, Object>> routeRanking(Integer limit);

    List<Map<String, Object>> userConsumptionRanking(Integer limit);

    List<Map<String, Object>> trainTrend(String trainNumber, Integer days);
}
