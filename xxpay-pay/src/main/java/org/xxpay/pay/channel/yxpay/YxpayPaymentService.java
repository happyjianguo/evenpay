package org.xxpay.pay.channel.yxpay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.kuxiong.SDKUtil;

import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-09-20 22:07:57
 * @description: YXPAY支付接口
 */
@Service
public class YxpayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(YxpayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_YXPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        YxpayConfig payChannelConfig = new YxpayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("out_trade_no", payOrder.getPayOrderId());//商户唯一订单号
        post.put("total_amount", (payOrder.getAmount()/100));//订单金额,单位元,2位小数
        post.put("order_name", payOrder.getSubject());//订单名称, 有效性视渠道支持
        post.put("notify_url", payConfig.getNotifyUrl(getChannelName()));//	支付接口异步通知地址
        post.put("return_url", payConfig.getReturnUrl(getChannelName()));//支付后前端跳转,只有在渠道支持的情况下才有效,如无需跳转传:http://not-return
        String content = JSON.toJSONString(post);

        HashMap<String, Object> body = new HashMap<String,Object>();
        body.put("app_id", payChannelConfig.getMchId());
        body.put("method", "alipay");
        body.put("version", "1.0");
        body.put("content",content);
        String sign = SDKUtil.getSign(body,payChannelConfig.getKey());
        body.put("sign",sign);
        body.put("sign_type", "MD5");
        HttpResponse result = HttpRequest.post(payChannelConfig.getReqUrl())
                .form(body)
                .execute();
        if (result.isOk()){
            JSONObject res =  JSONObject.parseObject(result.body());
            if (StrUtil.equals("error_code",res.getString("0"),true)) {
                int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null);
                _log.info("[{}] DouPay 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                JSONObject payInfo = new JSONObject();
                String payurl = res.getString("pay_url");
                payInfo.put("payUrl", payurl);
                payInfo.put("payMethod", PayConstant.PAY_METHOD_FORM_JUMP);
                retObj.put("payParams", payInfo);
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
            }else{
                _log.info(result.toString());
                retObj.put("errDes", "支付操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        }else{
            _log.info(result.toString());
            retObj.put("errDes", "支付操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
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
        YxpayConfig payChannelConfig = new YxpayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("out_trade_no", payOrder.getPayOrderId());//商户唯一订单号
        String content = JSON.toJSONString(post);

        HashMap<String, Object> body = new HashMap<String,Object>();
        body.put("app_id", payChannelConfig.getMchId());
        body.put("method", "trade.query");
        body.put("version", "1.0");
        body.put("content",content);
        String sign = SDKUtil.getSign(body,payChannelConfig.getKey());
        body.put("sign",sign);
        body.put("sign_type", "MD5");
        HttpResponse result = HttpRequest.post(payChannelConfig.getReqUrl())
                .form(body)
                .execute();
        if (result.isOk()){
            JSONObject res =  JSONObject.parseObject(result.body());
            _log.info("DouPay 查询订单返回数据:{}", res);
            if (StrUtil.equals("success",res.getString("status"),true)) {
                retObj.put("status", "2");//支付成功
                String eirthref = res.getString("trade_no");
                //上游渠道号更新
                retObj.put("channelOrderNo", eirthref);
            }else{
                retObj.put("status", "1");//支付中
            }
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
        }else{
            _log.info(result.toString());
            retObj.put("errDes", "查询操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
        }
        return retObj;
    }

}
