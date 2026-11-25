package com.mzlhsc.orpl.dish.service;

import com.mzlhsc.orpl.dish.domain.dto.StockChangeDTO;

public interface IDishStockService {

    /**
     * 查询剩余库存（Redis 未预热时用数据库值初始化）
     */
    int getStock(Long dishId);

    /**
     * 扣减库存：Redisson 锁 + 数据库条件更新，失败抛业务异常
     */
    void deduct(Long dishId, int number);

    /**
     * 回补库存（取消/拒单）
     */
    void restore(Long dishId, int number);

    /**
     * 设置绝对库存（管理端盘点/改库存）
     */
    void resetStock(Long dishId, int stock);
}
