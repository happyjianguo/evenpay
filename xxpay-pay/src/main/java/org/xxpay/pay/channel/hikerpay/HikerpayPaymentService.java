package org.xxpay.pay.channel.hikerpay;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;


/**
 * @author: gf
 * @date: 2019/07/19
 * @description: 海科支付接口
 */
@Service
public class HikerpayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(HikerpayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HKPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        JSONObject retObj;
        switch (channelId) {
            case PayConstant.PAY_CHANNEL_SWIFTPAY_WXPAY_NATIVE :
                retObj = doWxpayNativeReq(payOrder);
                break;
            case PayConstant.PAY_CHANNEL_SWIFTPAY_ALIPAY_NATIVE :
                retObj = doAlipayNativeReq(payOrder);
                break;
            case PayConstant.PAY_CHANNEL_SWIFTPAY_MICROPAY :
                retObj = doMicropayReq(payOrder);
                break;
            default:
                retObj = buildRetObj(PayConstant.RETURN_VALUE_FAIL, "不支持的渠道[channelId="+channelId+"]");
                break;
        }
        return retObj;
    }
}
