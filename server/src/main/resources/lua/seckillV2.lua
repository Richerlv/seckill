--解决redis事务不保证原子性的问题 --> 也就是把重复判断放在mysql层
--用户手机号
local seckillKey = KEYS[1]

--判断的流程:
local stock = tonumber(redis.call("get", seckillKey))
if stock > 0 then
    --减库存
    local res = redis.call("decr", seckillKey)
    if res == 1 then
        return 1
    else
        return -2
    end
else
    return 0
end
