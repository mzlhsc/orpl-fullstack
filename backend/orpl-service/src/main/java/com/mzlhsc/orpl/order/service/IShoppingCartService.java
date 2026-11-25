package com.mzlhsc.orpl.order.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mzlhsc.orpl.order.domain.dto.ShoppingCartDTO;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;


/**
 * <p>
 * 购物车 服务类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-13
 */
public interface IShoppingCartService extends IService<ShoppingCart> {

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    void addShoppingCart(ShoppingCartDTO shoppingCartDTO);

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
