package com.myproject.service.impl;

import com.myproject.entity.Result;
import com.myproject.mapper.TicketMapper;
import com.myproject.service.TicketService;
import com.myproject.util.AESUtil;
import com.myproject.util.CurreetHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TicketMapper ticketMapper;

    @Override
    public Result buy(TicketRequest req) {
        Long userId = CurreetHolder.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 身份证加密入库
        String encryptedIdNumber = AESUtil.encrypt(req.getPassengerIdNumber());

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("scheduleId", req.getScheduleId());
        params.put("passengerName", req.getPassengerName());
        params.put("passengerIdNumber", encryptedIdNumber);
        params.put("carriageId", req.getCarriageId());
        params.put("seatId", req.getSeatId());
        params.put("fromStationId", req.getFromStationId());
        params.put("toStationId", req.getToStationId());
        params.put("ticketPrice", req.getTicketPrice());
        params.put("seatType", req.getSeatType());
        params.put("scheduleDate", java.sql.Date.valueOf(req.getScheduleDate()));

        ticketMapper.bookTicket(params);

        int resultCode = (int) params.get("resultCode");
        String resultMsg = (String) params.get("resultMsg");

        if (resultCode != 0) {
            return Result.error(resultMsg);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", params.get("orderId"));
        data.put("resultMsg", resultMsg);
        return Result.success(data);
    }
}
