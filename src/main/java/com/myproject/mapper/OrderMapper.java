package com.myproject.mapper; 

import com.myproject.entity.Order; 
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 1. 下单：插入一条新的订单记录
     * @param order 订单对象（包含用户ID、车次ID、座位号、价格等）
     * @return 影响的行数（成功通常为1）
     */
    int insert(Order order);

    /**
     * 2. 查票/查订单：根据用户ID查询该用户的所有历史订单
     * @param userId 用户ID
     * @return 订单列表
     */
    List<Order> selectByUserId(@Param("userId") Long userId);

    /**
     * 3. 根据订单号查询详情（可选，用于支付回调或查看详情）
     * @param orderId 订单ID
     * @return 单个订单对象
     */
    Order selectById(@Param("orderId") Long orderId);

    /**
     * 4. 更新订单状态（例如：将“待支付”改为“已支付”，或者“已退票”）
     * @param orderId 订单ID
     * @param status 新的状态码
     * @return 影响的行数
     */
    int updateStatus(@Param("orderId") Long orderId, @Param("status") Integer status);
}
