package org.xxpay.pay.channel.hikerunion;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
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
import org.xxpay.core.entity.RefundOrder;
import org.xxpay.pay.channel.BaseRefund;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: gf
 * @date: 2019-08-01 10:05:17
 * @description: HIKERUNION退款
 */
@Service
public class HikerunionRefundService extends BaseRefund {

    private static final MyLog _log = MyLog.getLog(HikerunionRefundService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HIKERUNION;
    }

    public JSONObject refund(RefundOrder refundOrder) {
        HikerunionConfig payChannelConfig = new HikerunionConfig(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();

        //对方只支持纯数字订单号
        String orderId = HikerunionUtil.getOrder(refundOrder.getPayOrderId());

        Map<String, Object> post=new HashMap<String, Object>();
        //
        post.put("AMOUNT", String.format("%012d",refundOrder.getRefundAmount()));//总金额,单位分12位不足补零
        post.put("CLIENTREF",orderId); //商户订单号
        post.put("CLIENTKEY", payChannelConfig.getClientKey());
        post.put("FUSEACTION", "main.refundApi");
        post.put("EIRTHREF",refundOrder.getChannelPayOrderNo());
        post.put("LANGUAGECODE", "EN");
        post.put("CURRCODE","840");
        post.put("VERSION", "1.0.0");
        post.put("RETURNURL",payConfig.getNotifyUrl(getChannelName()));
        //加签
        String temp = PayDigestUtil.md5(payChannelConfig.getMchId()+"|"
                +post.get("EIRTHREF")+"|"
                +post.get("AMOUNT")+"|"
                +post.get("VERSION")+"|"
                +PayDigestUtil.md5(payChannelConfig.getKey(),"utf-8"),"utf-8");
        post.put("SIGNATURE", temp);
        String reqUrl = payChannelConfig.getReqUrl();
        _log.info("Hikerunion退款请求地址:{}", reqUrl);
        _log.info("Hikerunion退款请求数据:{}", post.toString());
        try{
            HttpResponse result = HttpRequest.post(reqUrl)
                    .form(post)
                    .execute();
            if (result.isOk()) {
                Map<String,String> resultMap = XmlUtils.toMap(result.bodyBytes(),"utf-8");
                String resultFlag = resultMap.get("resultflag");
                //0: General failure, 1: Refund succeeded, 2: Refund failed
                if(StrUtil.equals("1",resultFlag,true)){
                    retObj.put("obj", resultMap);
                    retObj.put("status", "0");
                    retObj.put("isSuccess", true);
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                }else {
                    retObj.put("errDes", "退款操作失败!");
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
                }
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


    /**
     * 查询退款订单
     * @param refundOrder
     * @return
     */
    @Override
    public JSONObject query(RefundOrder refundOrder) {
        //String channelId = payOrder.getChannelId();
        HikerunionConfig payChannelConfig = new HikerunionConfig(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();
        //String channelId = payOrder.getChannelId();
        //对方只支持纯数字订单号
        String orderId = HikerunionUtil.getOrder(refundOrder.getPayOrderId());

        Map<String, Object> post=new HashMap<String, Object>();
        post.put("CLIENTREF",orderId); //商户订单号
        post.put("CLIENTKEY", payChannelConfig.getClientKey());
        post.put("FUSEACTION", "main.inquiryApi");
        post.put("VERSION", "1.0.0");

        //加签
        String temp = PayDigestUtil.md5(payChannelConfig.getMchId()+"|"
                +post.get("CLIENTREF")+"|"
                +post.get("VERSION")+"|"
                +PayDigestUtil.md5(payChannelConfig.getKey(),"utf-8"),"utf-8");
        post.put("SIGNATURE", temp);
        String reqUrl = payChannelConfig.getReqUrl();
        _log.info("Hikerunion查询订单请求地址:{}", reqUrl);
        _log.info("Hikerunion查询订单请求数据:{}", post.toString());

        try{
            HttpResponse result = HttpRequest.post(reqUrl)
                    .form(post)
                    .execute();
            if (result.isOk()) {
                Map<String,String> resultMap = XmlUtils.toMap(result.bodyBytes(),"utf-8");
                String refundFlag = resultMap.get("refundflag");
                //0: General failure, 1: Payment succeeded, 2: Payment failed, 3: Payment pending.
                if(StrUtil.equals("1",refundFlag,true)){
                    retObj.put("status", "0");
                    retObj.put("isSuccess", true);
                    //商户订单号
                    retObj.put("channelOrderNo", refundOrder.getPayOrderId());
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                }else {
                    retObj.put("errDes", "退款操作失败!");
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
                    retObj.put("status", "1");
                }

            }else{
                retObj.put("errDes", "退款操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        } catch (Exception e) {
            retObj.put("errDes", "退款操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
        }
        return retObj;
    }
}
