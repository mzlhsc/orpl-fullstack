-- 2026-11-24 订单状态调整：删除“派送中(4)”，已接单可直接完成
-- 适用库：orpl；执行前请备份。重复执行安全。

USE `orpl`;

-- 历史派送中订单统一回退到已接单，由管理端直接完成
UPDATE `orders`
SET `status` = 3
WHERE `status` = 4;

-- 已完成订单统一为已支付，保证“完成即已支付”的数据口径
UPDATE `orders`
SET `pay_status` = 1
WHERE `status` = 5 AND `pay_status` <> 1;

-- 同步字段注释，去掉派送中状态
ALTER TABLE `orders`
MODIFY COLUMN `status` int NOT NULL DEFAULT '1'
COMMENT '订单状态 1待付款 2待接单 3已接单 5已完成 6已取消';
