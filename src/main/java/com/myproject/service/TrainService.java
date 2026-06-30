package com.myproject.service;

import com.myproject.entity.SeatInfo;
import com.myproject.entity.TrainSearchInfo;

import java.util.List;

public interface TrainService {
    List<TrainSearchInfo> search(Long fromStationId, Long toStationId, String date, String trainType);

    List<SeatInfo> findSeats(Long scheduleId, Long fromStationId, Long toStationId);
}
