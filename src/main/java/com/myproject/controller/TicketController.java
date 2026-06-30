package com.myproject.controller;

import com.myproject.entity.Result;
import com.myproject.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    public Result buy(@RequestBody TicketService.TicketRequest req) {
        return ticketService.buy(req);
    }
}
