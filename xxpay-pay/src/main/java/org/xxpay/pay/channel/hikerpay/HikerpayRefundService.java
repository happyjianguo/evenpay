package org.xxpay.pay.channel.hikerpay;

import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.pay.channel.BaseRefund;

/**
 * @author: dingzhiwei
 * @date: 2019/07/19
 * @description: 海科退款
 */
@Service
public class HikerpayRefundService extends BaseRefund {

    private static final MyLog _log = MyLog.getLog(HikerpayRefundService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HKPAY;
    }
}
