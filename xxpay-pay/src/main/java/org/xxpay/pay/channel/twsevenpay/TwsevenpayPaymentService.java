package org.xxpay.pay.channel.twsevenpay;

import com.alibaba.fastjson.JSONObject;

import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.twsevenpay.util.TwsevenpayUtil;
import java.util.HashMap;
import cn.hutool.http.HttpUtil;



/**
 * @author: gf
 * @date: 2019-07-29 15:18:30
 * @description: TWSEVENPAY支付接口
 */
@Service
public class TwsevenpayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(TwsevenpayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_TWSEVENPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        // String channelId = payOrder.getChannelId();
        TwsevenpayConfig payConfig = new TwsevenpayConfig(getPayParam(payOrder));
        String key = payConfig.getKey();
        String mchId = payConfig.getMchId();
        String orderId = payOrder.getPayOrderId();
        String reqURL = payConfig.getReqUrl();

        JSONObject retObj = new JSONObject();
        HashMap<String, Object> params = TwsevenpayUtil.buildPayParams(mchId, key, orderId, payOrder);
        reqURL = TwsevenpayUtil.buildPayURL(reqURL);
        try{
            String resp = HttpUtil.post(reqURL, params);
            _log.info("请求1177 pay api resp: {}", resp);
            // 判断177api返回结果是否成功
            JSONObject respJson = JSONObject.parseObject(resp);
            if (respJson.getIntValue("rc") > 0) throw new Exception("第三方接口失败: " + resp);
            int rc = rpcCommonService.rpcPayOrderService.updateStatus4Ing(
            		payOrder.getPayOrderId(), null);
            if (rc == 0) throw new Exception("更新订单状态失败");
            
            retObj.put("retCode", "SUCCESS");
        } catch (Exception e) {
            retObj.put("errDes", "支付操作失败: " + e.getMessage());
            return retObj;
        }
        _log.info("请求1177 pay api resp败: {}", "xxx");
        return retObj;
    }

    /**
     * 查询订单
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject query(PayOrder payOrder) {
        //String channelId = payOrder.getChannelId();
        
        JSONObject retObj = new JSONObject();
        return retObj;
    }


}
