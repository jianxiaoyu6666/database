package com.myproject.service.impl;

import com.myproject.entity.Order;
import com.myproject.entity.Train;
import com.myproject.mapper.OrderMapper;
import com.myproject.mapper.TrainMapper;
import com.myproject.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 必须导入这个
import java.util.Date;

@Service
public class TicketServiceImpl implements TicketService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 购票逻辑
     * @Transactional: 开启事务。如果下面任何一步报错，所有操作回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class) 
    public void buyTicket(Integer trainId, Integer userId) {
        // 1. 尝试扣减库存
        // 如果返回0，说明没票了（被并发抢光了），抛出异常触发回滚
        int rows = trainMapper.decreaseStock(trainId);
        if (rows == 0) {
            throw new RuntimeException("购票失败：余票不足");
        }

        // 2. 创建订单
        Order order = new Order();
        order.setTrainId(trainId);
        order.setUserId(userId);
        order.setCreateTime(new Date());
        
        // 插入订单到数据库
        orderMapper.insert(order); 
        
        // 如果这里发生异常（比如数据库挂了），上面的库存扣减也会自动回滚恢复
    }

    /**
     * 退票逻辑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundTicket(Integer orderId) {
        // 1. 查找订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 2. 删除或更新订单状态为已退票
        orderMapper.deleteOrCancel(orderId);

        // 3. 恢复库存
        trainMapper.increaseStock(order.getTrainId());
    }
}
