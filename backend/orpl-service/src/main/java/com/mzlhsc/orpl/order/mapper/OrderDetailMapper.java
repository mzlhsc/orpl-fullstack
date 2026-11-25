package com.mzlhsc.orpl.order.mapper;

import com.mzlhsc.orpl.order.domain.po.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderDetailMapper {

    /**
     * 根据订单id查询订单明细
     */
    @Select("select * from order_detail where order_id = #{orderId}")
    List<OrderDetail> getByOrderId(@Param("orderId") Long orderId);

    /**
     * 批量插入订单明细数据
     */
    void insertBatch(@Param("orderDetails") List<OrderDetail> orderDetails);
}
