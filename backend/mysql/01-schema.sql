
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `address_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号',
  `province_code` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='地址簿';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int DEFAULT NULL COMMENT '类型   1 菜品分类 2 套餐分类',
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` int DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品及套餐分类';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `pay_method` int NOT NULL DEFAULT '1',
  `pay_status` tinyint NOT NULL DEFAULT '1',
  `pay_time` datetime DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_payment_session` (`session_id`),
  KEY `idx_payment_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='堂食支付/AA记录';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_reservation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `table_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `contact_name` varchar(32) NOT NULL,
  `contact_phone` varchar(11) NOT NULL,
  `reserve_time` datetime NOT NULL,
  `guest_count` int NOT NULL DEFAULT '1',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1待确认 2已确认 3已到店 4已取消 5已过期',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_reservation_table_time` (`table_id`,`reserve_time`),
  KEY `idx_reservation_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='堂食预约';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_no` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话号，分享/加入用',
  `table_id` bigint NOT NULL COMMENT '桌台id',
  `table_name` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '主桌号快照',
  `order_id` bigint DEFAULT NULL COMMENT '关联账单订单id',
  `owner_user_id` bigint NOT NULL COMMENT '开台用户id',
  `guest_count` int NOT NULL DEFAULT '1' COMMENT '就餐人数',
  `status` int NOT NULL DEFAULT '1' COMMENT '1就餐中 2待结账 3已结账 4已关闭（原已取消）',
  `aa_enabled` tinyint NOT NULL DEFAULT '0' COMMENT '是否AA',
  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '账单累计金额',
  `remark` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `open_time` datetime DEFAULT NULL COMMENT '开台时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dining_session_no` (`session_no`),
  KEY `idx_dining_session_table` (`table_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='堂食就餐会话';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_session_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_session_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` bigint NOT NULL COMMENT '会话id',
  `user_id` bigint NOT NULL COMMENT '参与用户id',
  `nickname` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '昵称',
  `is_owner` tinyint NOT NULL DEFAULT '0' COMMENT '是否开台人 1是',
  `paid_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '已支付金额',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '0未付 1已付',
  `join_time` datetime DEFAULT NULL COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dining_member` (`session_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='堂食就餐成员';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_session_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_session_table` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `table_id` bigint NOT NULL,
  `is_primary` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_table` (`session_id`,`table_id`),
  KEY `idx_session_table_table` (`table_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='堂食会话与桌台映射（并桌）';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_table` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '桌号，如 A01',
  `seats` int NOT NULL DEFAULT '4' COMMENT '座位数',
  `area` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '区域，如 大厅/包间',
  `reservation_enabled` tinyint NOT NULL DEFAULT '1' COMMENT '允许预约',
  `status` int NOT NULL DEFAULT '0' COMMENT '0空闲 1就餐中 2待结账',
  `qr_token` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扫码开台令牌',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dining_table_name` (`name`),
  UNIQUE KEY `uk_dining_table_qr` (`qr_token`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='堂食桌台';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dining_table_operation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dining_table_operation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `from_table_id` bigint DEFAULT NULL,
  `to_table_id` bigint DEFAULT NULL,
  `operation_type` tinyint NOT NULL COMMENT '1转台 2并桌 3开台 4结账 5取消',
  `operator_id` bigint DEFAULT NULL,
  `operator_type` tinyint NOT NULL DEFAULT '1' COMMENT '1员工 2用户 3系统',
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_operation_session` (`session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='堂食桌台操作流水';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '菜品名称',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品价格',
  `image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述信息',
  `status` int DEFAULT '1' COMMENT '0 停售 1 起售',
  `stock` int NOT NULL DEFAULT '100' COMMENT '剩余库存',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `dish_flavor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint NOT NULL COMMENT '菜品',
  `name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '口味名称',
  `value` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '口味数据list',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜品口味关系表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `username` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `phone` varchar(11) COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8mb4_general_ci NOT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8mb4_general_ci NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工信息';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `order_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名字',
  `image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `dining_round` int NOT NULL DEFAULT '1' COMMENT '堂食下单轮次，1=第一次下单，外卖固定1',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `user_id` bigint DEFAULT NULL COMMENT '点菜用户id',
  `create_time` datetime DEFAULT NULL COMMENT '下单时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_type` int NOT NULL DEFAULT '1' COMMENT '1外卖 2堂食',
  `dining_session_id` bigint DEFAULT NULL COMMENT '堂食会话id',
  `number` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单号',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待付款 2待接单 3已接单 5已完成 6已取消',
  `user_id` bigint NOT NULL COMMENT '下单用户',
  `address_book_id` bigint DEFAULT NULL COMMENT '地址id（堂食为空）',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT '支付方式 1微信,2支付宝',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金额',
  `remark` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '地址',
  `user_name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '用户名称',
  `consignee` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` tinyint NOT NULL DEFAULT '1' COMMENT '配送状态  1立即送出  0选择具体时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间',
  `pack_amount` int DEFAULT NULL COMMENT '打包费',
  `tableware_number` int DEFAULT NULL COMMENT '餐具数量',
  `tableware_status` tinyint NOT NULL DEFAULT '1' COMMENT '餐具数量状态  1按餐量提供  0选择具体数量',
  PRIMARY KEY (`id`),
  KEY `idx_orders_dining_session` (`dining_session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=91 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `setmeal`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '菜品分类id',
  `name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL COMMENT '套餐价格',
  `status` int DEFAULT '1' COMMENT '状态 0:停用 1:启用',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_setmeal_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='套餐';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `setmeal_dish`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '菜品名称 （冗余字段）',
  `price` decimal(10,2) DEFAULT NULL COMMENT '菜品原价',
  `copies` int DEFAULT NULL COMMENT '份数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='套餐菜品关系';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `shopping_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片',
  `user_id` bigint NOT NULL COMMENT '主键',
  `dining_session_id` bigint DEFAULT NULL COMMENT '堂食会话id',
  `dish_id` bigint DEFAULT NULL COMMENT '菜品id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '套餐id',
  `dish_flavor` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '口味',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_cart_dining_session` (`dining_session_id`),
  KEY `idx_shopping_cart_dining_session` (`dining_session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='购物车';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT NULL COMMENT '注册时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户信息';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

