--商品Id
local orderKey = KEYS[1]
--用户手机号
local seckillKey = KEYS[2]
local value = ARGV[1]

--判断的流程:

--判断是否重复抢购
local order = redis.call("get", orderKey)
if order then
    --如果没重复，判断库存是否>0
    local stock = tonumber(redis.get("get", seckillKey))
    if stock > 0 then
        --减库存
        redis.call("decr", seckillKey)
        --下单
        local res = redis.call("setnx", orderKey, orderKey)
        if res == true then
            return 1
        else
            return 4
        end
    else
        return 3
    end
else
    return 2
end
