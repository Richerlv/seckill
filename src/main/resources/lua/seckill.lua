--商品Id
local orderKey = KEYS[1]
--用户手机号
local seckillKey = KEYS[2]

--判断的流程:

--判断是否重复抢购
local order = redis.call("get", orderKey)
if not order then
    --如果没重复，判断库存是否>0
    local stock = tonumber(redis.call("get", seckillKey))
    if stock > 0 then
        --减库存
        redis.call("decr", seckillKey)
        --下单
        local res = redis.call("setnx", orderKey, orderKey)
        if res == 1 then
            return 1
        else
            return 4
        end
    else
        return 0
    end
else
    return -1
end
