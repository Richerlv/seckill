-- 秒杀执行存储过程
DELIMITER $$  --console; 转换为$$

-- 定义存储过程
-- 参数: in 输入参数, out 输出参数
-- row_count(): 返回上一天修改类型sql的影响行数
-- row_count(): 0 未修改  >0 表示修改行数  <0:sql错误
CREATE DEFINER=`root`@`localhost` PROCEDURE `execute_seckill`(in v_seckill_id int, in v_phone varchar(255),
    in v_kill_time TIMESTAMP, in v_order_no varchar(255), out r_result int)
BEGIN
        DECLARE insert_count int DEFAULT 0;
START TRANSACTION;
INSERT ignore INTO successkilled
            (seckillId, userPhone, createTime, orderNo)
            VALUES (v_seckill_id, v_phone, v_kill_time, v_order_no);
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
