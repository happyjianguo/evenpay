package org.xxpay.pay.channel.kuxiong;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-08-15 17:57:17
 * @description: KUXIONG支付接口
 */
@Service
public class KuxiongPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(KuxiongPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_KUXIONG;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        KuxiongConfig payChannelConfig = new KuxiongConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();

        Map<String, Object> post=new HashMap<String, Object>();
        post.put("version", "V1.0");
        post.put("agentNo",payChannelConfig.getMchId());
        post.put("requestNo",payOrder.getPayOrderId());
        post.put("orderNo",payOrder.getPayOrderId());
        post.put("codeType","UNIONPAY");
        post.put("productType","QRCODE");
        post.put("transAmt",payOrder.getAmount());
        post.put("notifyUrl",payConfig.getNotifyUrl(getChannelName()));
        String signature = SDKUtil.getSign(post,payChannelConfig.getKey());
        post.put("signature",signature);
        String url = payChannelConfig.getReqUrl()+"/gateway/payment/upTransReq";
        HttpResponse result = HttpRequest.post(url)
                .form(post)
                .execute();
        if (result.isOk()) {
            String temp = result.body();
            Map<String, String> resData = SDKUtil.coverResultString2Map(temp);
            String codeImgUrl = resData.get("codeImgUrl");
            if (SDKUtil.isEmpty(codeImgUrl)){
                retObj = buildFailRetObj();
            }else{
                int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null,resData.toString());
                _log.info("[{}] KUXIONG 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                JSONObject payInfo = new JSONObject();
                payInfo.put("payUrl", codeImgUrl);
                payInfo.put("payMethod", PayConstant.PAY_METHOD_CODE_IMG);
                retObj.put("payParams", payInfo);
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
            }
        }else{
            retObj = buildFailRetObj();
        }

        return retObj;
    }


}
