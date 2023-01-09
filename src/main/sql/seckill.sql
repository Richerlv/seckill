/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80029
 Source Host           : localhost:3306
 Source Schema         : seckill

 Target Server Type    : MySQL
 Target Server Version : 80029
 File Encoding         : 65001

 Date: 07/01/2023 15:26:07
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for seckill
-- ----------------------------
DROP TABLE IF EXISTS `seckill`;
CREATE TABLE `seckill`  (
  `seckillId` int NOT NULL AUTO_INCREMENT COMMENT '商品Id',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名',
  `number` int NOT NULL COMMENT '库存',
  `start` timestamp NOT NULL COMMENT '秒杀开始时间',
  `end` timestamp NOT NULL COMMENT '秒杀结束时间',
  `createTime` timestamp NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`seckillId`) USING BTREE,
  INDEX `start_idx`(`start` ASC) USING BTREE,
  INDEX `end_idx`(`end` ASC) USING BTREE,
  INDEX `create_idx`(`createTime` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of seckill
-- ----------------------------
INSERT INTO `seckill` VALUES (1, '1000元秒杀iphone14', 100, '2023-01-02 00:00:00', '2023-01-03 00:00:00', '2023-01-01 00:00:00');
INSERT INTO `seckill` VALUES (2, '500元秒杀ipad2', 89, '2023-01-02 00:00:00', '2023-02-04 00:00:00', '2023-01-01 00:00:00');
INSERT INTO `seckill` VALUES (3, '300元秒杀华为P50', 299, '2023-01-07 00:00:00', '2023-01-20 00:00:00', '2023-01-01 00:00:00');
INSERT INTO `seckill` VALUES (4, '200元秒杀mac', 400, '2023-01-02 00:00:00', '2023-01-03 00:00:00', '2023-01-01 00:00:00');

-- ----------------------------
-- Table structure for successkilled
-- ----------------------------
DROP TABLE IF EXISTS `successkilled`;
CREATE TABLE `successkilled`  (
  `seckillId` int NOT NULL COMMENT '商品id，与用户手机号构成联合主键',
  `userPhone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户手机号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态：0：正常 -1：无效 1：已付款',
  `createTime` timestamp NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`seckillId`, `userPhone`) USING BTREE,
  INDEX `create_idx`(`createTime` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of successkilled
-- ----------------------------
INSERT INTO `successkilled` VALUES (2, '111111111111', 0, '2023-01-04 14:08:35');
INSERT INTO `successkilled` VALUES (2, '12', 0, '2023-01-03 21:13:44');
INSERT INTO `successkilled` VALUES (2, '12345678956', 0, '2023-01-05 14:20:34');
INSERT INTO `successkilled` VALUES (2, '12428751235', 0, '2023-01-05 22:00:27');
INSERT INTO `successkilled` VALUES (2, '127', 0, '2023-01-03 21:25:18');
INSERT INTO `successkilled` VALUES (2, '13485971245', 0, '2023-01-06 14:13:34');
INSERT INTO `successkilled` VALUES (2, '15245698596', 0, '2023-01-06 14:08:57');
INSERT INTO `successkilled` VALUES (2, '15667462251', 0, '2023-01-05 22:01:26');

-- ----------------------------
-- Procedure structure for execute_seckill
-- ----------------------------
DROP PROCEDURE IF EXISTS `execute_seckill`;
delimiter ;;
CREATE PROCEDURE `execute_seckill`(in v_seckill_id int, in v_phone varchar(255),
    in v_kill_time TIMESTAMP, out r_result int)
BEGIN
        DECLARE insert_count int DEFAULT 0;
        START TRANSACTION;
        INSERT ignore INTO successkilled
            (seckillId, userPhone, createTime)
            VALUES (v_seckill_id, v_phone, v_kill_time);
        SELECT ROW_COUNT() INTO insert_count;
        IF (insert_count = 0) THEN
            ROLLBACK;
            SET r_result = -1;
        ELSEIF (insert_count < 0) THEN
            ROLLBACK;
            SET r_result = -2;
        ELSE
            UPDATE seckill
            SET number = number - 1
            WHERE seckillId = v_seckill_id
              AND v_kill_time > `start`
              AND `end` > v_kill_time
              AND number > 0;
        SELECT ROW_COUNT() INTO insert_count;
        IF(insert_count = 0) THEN
            ROLLBACK;
            SET r_result = 0;
        ELSEIF(insert_count < 0) THEN
            ROLLBACK ;
            SET r_result = -2;
        ELSE
                COMMIT;
                SET r_result = 1;
        END IF;
    END IF;
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
