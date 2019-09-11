package org.xxpay.pay.channel.dragonPay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-09-11 11:01:12
 * @description: DRAGONPAY支付接口
 */
@Service
public class DragonPayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(DragonPayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_DRAGONPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        DragonPayConfig payChannelConfig = new DragonPayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("merchantid",payChannelConfig.getMchId());
        SimpleDateFormat format=new SimpleDateFormat("yyyyMMddHHmmss");
        String time=format.format(new Date());
        post.put("merc_ord_no", payOrder.getPayOrderId());
        post.put("version","1.0");
        post.put("tranamt", (payOrder.getAmount()*100));
        post.put("notifyurl", payConfig.getNotifyUrl(getChannelName()));
        post.put("callbackurl", payConfig.getJumpUrl());
        post.put("paytype", "ALIPAYWAP");
        post.put("reqtime", time);
        try {
            String sign = PayDigestUtil.getRSASign(post,payChannelConfig.getKey());
            post.put("sign", sign);
            HttpResponse result = HttpRequest.post(payChannelConfig.getReqUrl()+"/single/scan")
                    .form(post)
                    .execute();
            if (result.isOk()) {
                JSONObject res = JSONObject.parseObject(result.body());
                if (StrUtil.equals(res.getString("returncode"),"0000")) {
                    int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null);
                    _log.info("[{}] Dragonpay 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                    JSONObject payInfo = new JSONObject();
                    payInfo.put("payUrl", res.getString("codeurl"));
                    payInfo.put("payMethod", PayConstant.PAY_METHOD_FORM_JUMP);
                    retObj.put("payParams", payInfo);
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                }else{
                    _log.info(result.toString());
                    retObj.put("errDes", res.getString("returnmsg"));
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
                }
            }else{
                _log.info(result.toString());
                retObj.put("errDes", "支付操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        }catch (Exception e){
            _log.error(e.getMessage());
            retObj.put("errDes", "支付操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            return retObj;
        }
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
        DragonPayConfig payChannelConfig = new DragonPayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        String orderId = payOrder.getPayOrderId();
        
        return retObj;
    }


}
