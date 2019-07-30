package org.xxpay.pay.channel.twsevenpay;


import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayNotify;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;
import org.xxpay.pay.util.Util;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-07-29 15:18:30
 * @description: TWSEVENPAY支付回调
 */
@Service
public class TwsevenpayPayNotifyService extends BasePayNotify {

    private static final MyLog _log = MyLog.getLog(TwsevenpayPayNotifyService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_TWSEVENPAY;
    }

    @Override
    public JSONObject doNotify(Object notifyData) {
        String logPrefix = "【处理海科支付回调】";
        _log.info("====== 开始处理海科支付回调通知 ======");
        HttpServletRequest req = (HttpServletRequest) notifyData;
        JSONObject retObj = buildRetObj();
        Map<String, Object> payContext = new HashMap();
        PayOrder payOrder;
        String respString = PayConstant.RETURN_SWIFTPAY_VALUE_FAIL;
        try {
        }catch (Exception e) {
            _log.error(e, logPrefix + "海科支付退款处理异常");
        }
        retObj.put(PayConstant.RESPONSE_RESULT, respString);
        return retObj;
    }

    /**
     * 验证海科支付通知参数
     * @return
     */
    public boolean verifyPayParams(Map<String, Object> payContext) {
        return true;
    }

    //验证签名
    private boolean checkParam (JSONObject params,String key){
        return true;
    }
}
