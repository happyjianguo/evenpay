package org.xxpay.pay.channel.ympay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.common.util.XXPayUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-08-10 15:30:45
 * @description: YMPAY支付接口
 */
@Service
public class YmpayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(YmpayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_YMPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        YmpayConfig payChannelConfig = new YmpayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        Map<String, Object> post=new HashMap<String, Object>();

        post.put("version", "1");
        post.put("merNo",payChannelConfig.getMchId());
        post.put("orderNo",payOrder.getPayOrderId());
        post.put("orderAmount",payOrder.getAmount());
        post.put("timeStamp",System.currentTimeMillis()/1000);
        post.put("callbackUrl",payConfig.getNotifyUrl(getChannelName()));
        post.put("returnUrl",payConfig.getJumpUrl());
        post.put("goodsName",payOrder.getBody());
        switch (channelId) {
            case PayConstant.PAY_CHANNEL_YMPAY_ALI_MWEB :
                post.put("payType","4");
                post.put("bankCode","alipay");
                post.put("sign",PayDigestUtil.getSign(post,payChannelConfig.getKey()).toUpperCase());
                break;
            case PayConstant.PAY_CHANNEL_YMPAY_CARD :
                post.put("payType","6");
                post.put("bankCode","jiaotong");
                post.put("sign",PayDigestUtil.getSign(post,payChannelConfig.getKey()).toUpperCase());
                //快捷支付直接返回拼接的请求url
                String reqUrl = payChannelConfig.getReqUrl() + "/pay/index?" + XXPayUtil.genUrlParams(post);
                _log.info("YmPay支付请求地址:{}", reqUrl);
                int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null,reqUrl);
                _log.info("[{}] YmPay 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                //支付链接返回给下级
                JSONObject payInfo = new JSONObject();
                payInfo.put("payUrl", reqUrl);
                payInfo.put("payMethod", PayConstant.PAY_METHOD_FORM_JUMP);
                retObj.put("payParams", payInfo);
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                return retObj;
            default:
                retObj = buildRetObj(PayConstant.RETURN_VALUE_FAIL, "不支持的渠道[channelId="+channelId+"]");
                break;
        }
        String reqUrl = payChannelConfig.getReqUrl()+"/pay/index";
        try{
            HttpResponse result = HttpRequest.post(reqUrl)
                    .form(post)
                    .execute();
            if (result.getStatus() == 302) {
                //如果保存返回结果集会导致超长
                int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null);
                _log.info("[{}] Hikerpass 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                JSONObject payInfo = new JSONObject();
                payInfo.put("payUrl", result.header("location"));
                payInfo.put("payMethod", PayConstant.PAY_METHOD_FORM_JUMP);
                retObj.put("payParams", payInfo);
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
            }else{
                retObj.put("errDes", "支付操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        } catch (Exception e) {
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
        YmpayConfig payChannelConfig = new YmpayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        String orderId = payOrder.getPayOrderId();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("version", "1");
        post.put("merNo",payChannelConfig.getMchId());
        post.put("orderNo",orderId);
        post.put("orderAmount",payOrder.getAmount());
        post.put("timeStamp",System.currentTimeMillis()/1000);
        post.put("type","1");
        post.put("sign",PayDigestUtil.getSign(post,payChannelConfig.getKey()).toUpperCase());

        String reqUrl = payChannelConfig.getReqUrl()+"/query/order";
        _log.info("Ympay查询订单请求地址:{}", reqUrl);
        _log.info("Ympay查询订单请求数据:{}", post.toString());

        try{
            HttpResponse result = HttpRequest.post(reqUrl)
                    .form(post)
                    .execute();
            if (result.isOk()) {
                JSONObject res =  JSONObject.parseObject(result.body());
                _log.info("Ympay查询订单返回数据:{}", res);
                String code = res.getString("code");
                //0: General failure, 1: Payment succeeded, 2: Payment failed, 3: Payment pending.
                if(StrUtil.equals("0x001",code,true)){
                    retObj.put("status", "2");//支付成功
                    _log.info("Ympay查询订单支付成功");
                }else {
                    retObj.put("status", "1");//支付中
                    _log.info("Ympay查询订单支付中");
                }
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
            }else{
                retObj.put("errDes", "查询操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        } catch (Exception e) {
            retObj.put("errDes", "查询操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
        }
        return retObj;
    }


}
