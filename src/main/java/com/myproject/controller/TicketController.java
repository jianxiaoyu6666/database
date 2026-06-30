package com.myproject.controller;

import com.myproject.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/buy")
    public String buy(@RequestParam Integer trainId, @RequestParam Integer userId) {
        try {
            ticketService.buyTicket(trainId, userId);
            return "购票成功";
        } catch (Exception e) {
            return "购票失败: " + e.getMessage();
        }
    }
    
    @PostMapping("/refund")
    public String refund(@RequestParam Integer orderId) {
         ticketService.refundTicket(orderId);
         return "退票成功";
    }
}
