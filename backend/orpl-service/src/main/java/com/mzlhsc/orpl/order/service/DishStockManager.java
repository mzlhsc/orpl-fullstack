package com.mzlhsc.orpl.order.service;

import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.dish.service.IDishStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单侧的菜品库存操作：直接调用菜品领域的库存服务。
 * <p>
 * 任意一步失败都会把本次已经扣减的数量全部回补，避免下单失败却吃掉库存。
 * 真正的并发安全由菜品服务的 Redisson 锁 + 条件更新 SQL 保证。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DishStockManager {

    private final IDishStockService dishStockService;

    public void deductDishes(List<StockItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<StockItem> done = new ArrayList<>();
        try {
            for (StockItem item : items) {
                if (item.dishId() == null || item.number() <= 0) {
                    continue;
                }
                dishStockService.deduct(item.dishId(), item.number());
                done.add(item);
            }
        } catch (Exception e) {
            log.warn("扣减菜品库存失败，开始回补已扣减部分: {}", e.getMessage());
            for (StockItem item : done) {
                try {
                    dishStockService.restore(item.dishId(), item.number());
                } catch (Exception ex) {
                    log.error("库存回补失败 dishId={} number={}", item.dishId(), item.number(), ex);
                }
            }
            if (e instanceof BizIllegalException biz) {
                throw biz;
            }
            throw new BizIllegalException("菜品库存扣减失败，请稍后重试", e);
        }
    }

    public void restore(Long dishId, Integer number) {
        if (dishId == null || number == null || number <= 0) {
            return;
        }
        try {
            dishStockService.restore(dishId, number);
        } catch (Exception e) {
            log.error("库存回补失败 dishId={} number={}", dishId, number, e);
        }
    }

    public record StockItem(Long dishId, Integer number, String name) {
    }
}
