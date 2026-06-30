package com.myproject.mapper;

import com.myproject.entity.station;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StationMapper {
    List<station> searchByKeyWord(@Param("keyword") String keyword);
}
