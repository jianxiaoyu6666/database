package com.myproject.service;

public interface TicketService {
    // 购票方法
    void buyTicket(Integer trainId, Integer userId);
    
    // 退票方法
    void refundTicket(Integer orderId);
}
