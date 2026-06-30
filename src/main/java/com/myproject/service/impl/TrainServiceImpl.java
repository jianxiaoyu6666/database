package com.myproject.service.impl;

import com.myproject.entity.SeatInfo;
import com.myproject.entity.TrainSearchInfo;
import com.myproject.mapper.TrainMapper;
import com.myproject.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TrainServiceImpl implements TrainService {
    @Autowired
    private TrainMapper trainMapper;

    @Override
    public List<TrainSearchInfo> search(Long fromStationId, Long toStationId, String date, String trainType) {
        if (fromStationId == null) throw new RuntimeException("起点站ID不可以为空");
        if (toStationId == null) throw new RuntimeException("终点站ID不可以为空");
        if (date == null) throw new RuntimeException("日期不可以为空");
        return trainMapper.findByInfo(fromStationId, toStationId, LocalDate.parse(date), trainType);
    }

    @Override
    public List<SeatInfo> findSeats(Long scheduleId, Long fromStationId, Long toStationId) {
        if (scheduleId == null) throw new RuntimeException("排班ID不可以为空");
        if (fromStationId == null) throw new RuntimeException("起点站ID不可以为空");
        if (toStationId == null) throw new RuntimeException("终点站ID不可以为空");
        return trainMapper.findAvailableSeats(scheduleId, fromStationId, toStationId);
    }
}
