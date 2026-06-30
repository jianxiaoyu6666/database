package com.myproject.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.Map;

@Mapper
public interface TicketMapper {

    /**
     * 购票 — 调用 sp_book_ticket 存储过程
     * 传入 params Map，OUT 参数 resultCode/resultMsg/orderId 会回填到同一个 Map 里
     */
    void bookTicket(Map<String, Object> params);
}
