-- 堂食同一订单支持多次加餐：订单明细记录下单轮次
-- 1=第一次下单，2=第二次下单；外卖与历史数据默认 1
USE orpl;

SET @column_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'order_detail'
    AND COLUMN_NAME = 'dining_round'
);
SET @ddl = IF(
  @column_exists = 0,
  'ALTER TABLE `order_detail` ADD COLUMN `dining_round` int NOT NULL DEFAULT 1 COMMENT ''堂食下单轮次，1=第一次下单，外卖固定1'' AFTER `order_id`',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `order_detail` SET `dining_round` = 1 WHERE `dining_round` IS NULL OR `dining_round` < 1;
