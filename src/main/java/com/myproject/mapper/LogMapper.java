package com.myproject.mapper;

import com.myproject.entity.audit_log;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LogMapper {
    @Insert("insert into audit_log() values ()")
    void insert();

    @Update("SET @audit_operator = #{operatorId}")
    void setAuditOperator(@Param("operatorId") Long operatorId);

    // 按条件查审计日志（后面写Controller用）
    List<audit_log> list(@Param("tableName") String tableName,
                         @Param("operationType") String operationType,
                         @Param("startDate") String startDate,
                         @Param("endDate") String endDate,
                         @Param("operatedBy") Long operatedBy);
}
