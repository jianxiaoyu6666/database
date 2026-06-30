package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/buy")
    public Result buy(@RequestParam Integer trainId, @RequestParam Integer userId) {
        try {
            ticketService.buyTicket(trainId, userId);
            return Result.success("购票成功");
        } catch (Exception e) {
            return Result.error("购票失败: " + e.getMessage());
        }
    }

    @PostMapping("/refund")
    public Result refund(@RequestParam Integer orderId) {
        try {
            ticketService.refundTicket(orderId);
            return Result.success("退票成功");
        } catch (Exception e) {
            return Result.error("退票失败: " + e.getMessage());
        }
    }
}
