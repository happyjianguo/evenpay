package org.xxpay.pay.channel.hikerunion;

import cn.hutool.core.date.DateUtil;

public class HikerunionUtil {
    //因对方只支持不大于18位的数字订单号。因此需对原订单号进行改造。去除前面的前缀
    static String getOrder (String oldOrder) {
        String newOrder = oldOrder.replace("P01"+DateUtil.thisYear(),"");
        return newOrder;
    }

    //还原id号
    static String reduOrder (String oldOrder) {
        String newOrder = "P01"+DateUtil.thisYear()+oldOrder;
        return newOrder;
    }

}
