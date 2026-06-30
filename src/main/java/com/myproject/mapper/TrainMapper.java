package com.myproject.mapper;

import com.myproject.entity.SeatInfo;
import com.myproject.entity.TrainSearchInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TrainMapper {

    List<TrainSearchInfo> findByInfo(@Param("fromStationId") Long fromStationId,
                                     @Param("toStationId") Long toStationId,
                                     @Param("date") LocalDate date,
                                     @Param("trainType") String trainType);

    List<SeatInfo> findAvailableSeats(@Param("scheduleId") Long scheduleId,
                                       @Param("fromStationId") Long fromStationId,
                                       @Param("toStationId") Long toStationId);
}
