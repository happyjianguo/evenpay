package org.xxpay.pay.channel.hikerpay;

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
import org.xxpay.core.common.util.JsonUtil;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.RefundOrder;
import org.xxpay.pay.channel.BaseRefund;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;

import java.io.IOException;

/**
 * @author: gf
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

    public JSONObject refund(RefundOrder refundOrder) {
        HikerpayConfig hikertpayConfig = new HikerpayConfig(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();
        JSONObject param = new JSONObject();
        String CREDENTIAL_CODE = hikertpayConfig.getKey();
        String PARTNER_CODE = hikertpayConfig.getMchId();
        // 商户单号
        String orderId = refundOrder.getPayOrderId();
        // 商户退款单号
        String refundId = refundOrder.getRefundOrderId();
        // 退款金额
        param.put("fee", String.valueOf(refundOrder.getRefundAmount()));
        String reqUrl = hikertpayConfig.getReqUrl()
                + "/gateway/partners/"
                + PARTNER_CODE
                + "/orders/"
                + orderId
                + "/refunds/"
                + refundId + "?"
                + HikerUtil.queryParams(System.currentTimeMillis(),PARTNER_CODE,CREDENTIAL_CODE);
        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try {
            String req = param.toJSONString();
            HttpPut httpPut = new HttpPut(reqUrl);
            _log.info("HikerRefund请求数据:{}", reqUrl);
            StringEntity entityParams = new StringEntity(req, "utf-8");
            httpPut.setEntity(entityParams);
            httpPut.setHeader("Content-Type", "application/json");
            client = HttpClients.createDefault();
            response = client.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();
            if(response != null && (statusCode -200 <100 )){
                HttpEntity entity = response.getEntity();
                String result =  EntityUtils.toString(entity);
                _log.info("Hikerpass退款请求结果:{}", result);
                JSONObject res =  JSONObject.parseObject(result);
                String resultCode =  res.getString("result_code");
                if (resultCode.equalsIgnoreCase("FINISHED")) {
                    retObj.put("obj", res);
                    retObj.put("status", "0");
                    retObj.put("isSuccess", true);
                    //商户订单号
                    retObj.put("channelOrderNo", orderId);
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                }else {
                    retObj.put("errDes", "退款操作失败!");
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
                }
            }else{
                retObj.put("errDes", "退款操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        } catch (Exception e) {
            _log.error(e, "");
            retObj.put("errDes", "退款操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            return retObj;
        } finally {
            if(client != null){
                try {
                    client.close();
                } catch (IOException e) {
                    _log.error(e, "");
                }
            }
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
        HikerpayConfig hikertpayConfig = new HikerpayConfig(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();
        String CREDENTIAL_CODE = hikertpayConfig.getKey();
        String PARTNER_CODE = hikertpayConfig.getMchId();
        String orderId = refundOrder.getPayOrderId();
        String refundId = refundOrder.getRefundOrderId();
        String reqUrl = hikertpayConfig.getReqUrl()
                + "/gateway/partners/"
                + PARTNER_CODE
                + "/orders/"
                + orderId
                + "/refunds/"
                + refundId + "?"
                + HikerUtil.queryParams(System.currentTimeMillis(),PARTNER_CODE,CREDENTIAL_CODE);

        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try {
            HttpGet httpGet = new HttpGet(reqUrl);
            _log.info("HikerQuery请求数据:{}", reqUrl);

            client = HttpClients.createDefault();
            response = client.execute(httpGet);

            if(response != null && (response.getStatusLine().getStatusCode() -200 <100 )){
                HttpEntity entity = response.getEntity();
                String result =  EntityUtils.toString(entity);
                _log.info("Hikerpass请求结果:{}", result);
                JSONObject res =  JSONObject.parseObject(result);
                if ("SUCCESS".equals(res.getString("return_code"))) {
                    // 交易状态
                    /*
                        • PAYING: Waiting for payment
                        • CREATE_FAIL: Fail to create order
                        • CLOSED: Order closed
                        • PAY_FAIL: Payment failed
                        • PAY_SUCCESS: Payment succeeded
                        • PARTIAL_REFUND: Partial refunded
                        • Full_REFUND: Full refunded
                        Use the same order id to call create order API can renew the order. PAYING,
                        PAY_SUCCESS orders cannot be renewed.
                    */
                    String trade_state = res.getString("result_code");
                    retObj.put("obj", res);
                    if("PARTIAL_REFUND".equals(trade_state) || "Full_REFUND".equals(trade_state)){
                        retObj.put("status", "0");
                    }else {
                        retObj.put("status", "1");
                    }
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                }else {
                    retObj.put("errDes", "退款查询操作失败!");
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
                }
            }else{
                retObj.put("errDes", "退款查询操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        } catch (Exception e) {
            _log.error(e, "");
            retObj.put("errDes", "退款查询操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            return retObj;
        } finally {
            if(client != null){
                try {
                    client.close();
                } catch (IOException e) {
                    _log.error(e, "");
                }
            }
        }
        return retObj;
    }
}
