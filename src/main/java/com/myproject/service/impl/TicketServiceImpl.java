package com.myproject.service.impl;

import com.myproject.entity.Result;
import com.myproject.mapper.OrderMapper;
import com.myproject.mapper.TicketMapper;
import com.myproject.mapper.TrainScheduleMapper;
import com.myproject.service.TicketService;
import com.myproject.util.AESUtil;
import com.myproject.util.CurreetHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private TrainScheduleMapper trainScheduleMapper;

    // ==================== 购票（原有方法，保持风格一致） ====================

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

    // ==================== 退票（新增） ====================

    @Override
    @Transactional
    public Result cancelOrder(Long orderId) {
        Long userId = CurreetHolder.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 1. 查询订单
        Map<String, Object> order = orderMapper.findById(orderId);
        if (order == null || order.isEmpty()) {
            return Result.error("订单不存在");
        }

        // 2. 检查权限（只能退自己的票）
        Long orderUserId = (Long) order.get("userId");
        if (!orderUserId.equals(userId)) {
            log.warn("用户 {} 尝试退他人的订单 {}", userId, orderId);
            return Result.error("无权操作此订单");
        }

        // 3. 检查订单状态
        Integer status = (Integer) order.get("status");
        if (status == 3) {
            return Result.error("该订单已退票");
        }
        if (status == 0) {
            return Result.error("该订单尚未支付，请先支付或等待自动取消");
        }
        if (status == 4) {
            return Result.error("该订单已过期，无法退票");
        }
        if (status == 5) {
            return Result.error("该订单已取消，无法退票");
        }
        if (status == 2) {
            return Result.error("该车票已使用，无法退票");
        }

        // 4. 检查是否已发车
        Long scheduleId = (Long) order.get("scheduleId");
        java.sql.Date departureDate = trainScheduleMapper.getDepartureDate(scheduleId);
        if (departureDate == null) {
            return Result.error("车次信息不存在");
        }

        // 判断是否已发车（只比较日期，不比较具体时间）
        Date today = Date.valueOf(LocalDateTime.now().toLocalDate());
        if (departureDate.before(today)) {
            return Result.error("列车已发车，无法退票");
        }

        // 如果发车时间在2小时内，也不允许退票（或收取高额手续费）
        // 这里只检查日期，如果需要精确到小时，可以扩展

        // 5. 计算退票手续费
        BigDecimal ticketPrice = (BigDecimal) order.get("ticketPrice");
        BigDecimal refundAmount = calculateRefundAmountByDate(departureDate, ticketPrice);

        // 6. 调用存储过程退票
        Map<String, Object> params = new HashMap<>();
        params.put("p_order_id", orderId);
        params.put("p_refund_amount", refundAmount);
        params.put("p_user_id", userId);

        ticketMapper.cancelTicket(params);

        int resultCode = (int) params.get("p_result_code");
        String resultMsg = (String) params.get("p_result_msg");

        if (resultCode != 0) {
            return Result.error(resultMsg);
        }

        log.info("退票成功: orderId={}, userId={}, refundAmount={}", 
                 orderId, userId, refundAmount);

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("refundAmount", refundAmount);
        data.put("refundTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return Result.success(data);
    }

    // ==================== 查询我的订单列表（新增） ====================

    @Override
    public Result getMyOrders(Integer page, Integer size, Integer status) {
        Long userId = CurreetHolder.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        int offset = (page - 1) * size;

        // 查询订单列表
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("status", status);
        params.put("offset", offset);
        params.put("size", size);

        List<Map<String, Object>> orders = orderMapper.findByUserId(params);
        int total = orderMapper.countByUserId(userId, status);

        // 解密身份证号
        for (Map<String, Object> order : orders) {
            String encryptedIdNumber = (String) order.get("passengerIdNumber");
            if (encryptedIdNumber != null) {
                String decrypted = AESUtil.decrypt(encryptedIdNumber);
                order.put("passengerIdNumber", decrypted);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", orders);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (total + size - 1) / size);

        return Result.success(result);
    }

    // ==================== 查询订单详情（新增） ====================

    @Override
    public Result getOrderDetail(Long orderId) {
        Long userId = CurreetHolder.getCurrentId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        Map<String, Object> order = orderMapper.findById(orderId);
        if (order == null || order.isEmpty()) {
            return Result.error("订单不存在");
        }

        Long orderUserId = (Long) order.get("userId");
        if (!orderUserId.equals(userId)) {
            return Result.error("无权查看此订单");
        }

        // 解密身份证号
        String encryptedIdNumber = (String) order.get("passengerIdNumber");
        if (encryptedIdNumber != null) {
            String decrypted = AESUtil.decrypt(encryptedIdNumber);
            order.put("passengerIdNumber", decrypted);
        }

        // 查询关联的车票信息
        List<Map<String, Object>> tickets = ticketMapper.findByOrderId(orderId);

        Map<String, Object> detail = new HashMap<>();
        detail.put("order", order);
        detail.put("tickets", tickets);

        return Result.success(detail);
    }

   
    
}
