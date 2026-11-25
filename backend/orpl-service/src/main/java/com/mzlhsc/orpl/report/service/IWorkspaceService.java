package com.mzlhsc.orpl.report.service;

import com.mzlhsc.orpl.dish.domain.vo.DishOverViewVO;
import com.mzlhsc.orpl.order.domain.vo.OrderOverViewVO;
import com.mzlhsc.orpl.setmeal.domain.vo.SetmealOverViewVO;
import com.mzlhsc.orpl.report.domain.vo.BusinessDataVO;

import java.time.LocalDateTime;

public interface IWorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     */
    BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end);

    /**
     * 查询订单管理数据
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询菜品总览
     */
    DishOverViewVO getDishOverView();

    /**
     * 查询套餐总览
     */
    SetmealOverViewVO getSetmealOverView();

}

