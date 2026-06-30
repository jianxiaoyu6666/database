package com.myproject.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.myproject.entity.Train;

@Mapper
public interface TrainMapper {

    // 查询车次信息
    Train selectById(@Param("id") Integer id);

    /**
     * 核心：扣减库存
     * 使用 WHERE stock > 0 保证不会扣成负数（并发控制的关键）
     */
    @Update("UPDATE train SET stock = stock - 1 WHERE id = #{id} AND stock > 0")
    int decreaseStock(@Param("id") Integer id);

    /**
     * 增加库存（退票/补加车厢时调用）
     * id：车次主键
     * 返回受影响行数，成功返回1，无此车次返回0
     */
    @Update("UPDATE train SET stock = stock + 1 WHERE id = #{id}")
    int increaseStock(@Param("id") Integer id);
}
