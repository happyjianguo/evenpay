package org.xxpay.pay.channel.hikerpay;


import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.pay.channel.BasePayNotify;


/**
 * @author: gf
 * @date: 2018/07/19
 * @description: 海科支付回调
 */
@Service
public class HikerpayNotifyService extends BasePayNotify {

    private static final MyLog _log = MyLog.getLog(HikerpayNotifyService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HKPAY;
    }
}
