package org.xxpay.pay.channel.itopay;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;
import java.io.IOException;


/**
 * @author: gf
 * @date: 2019-07-29 17:42:31
 * @description: ITOPAY支付接口
 */
@Service
public class ItopayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(ItopayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_ITOPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        ItopayConfig payChannelConfig = new ItopayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        JSONObject post = new JSONObject();
        String orderId = payOrder.getPayOrderId();
        // 总金额,单位分
        post.put("paymoney", String.valueOf(payOrder.getAmount()));
        post.put("siteid", payChannelConfig.getMchId());//商户号
        post.put("goodsname",payOrder.getBody());
        post.put("client_ip",payOrder.getClientIp());
        post.put("thereport_url",payConfig.getNotifyUrl(getChannelName()));
        post.put("thejump_url",payConfig.getJumpUrl());
        post.put("nowinttime", System.currentTimeMillis()/1000);//时间戳
        switch (channelId) {
            case PayConstant.PAY_CHANNEL_ITOPAY_WX_MWEB:
                post.put("paytypeid", "21");
                break;
            case PayConstant.PAY_CHANNEL_ITOPAY_ALI_MWEB:
                post.put("paytypeid", "22");
                break;
            default:
                retObj = buildRetObj(PayConstant.RETURN_VALUE_FAIL, "不支持的渠道[channelId=" + channelId + "]");
                break;
        }

        String temp = post.get("siteid") +
                "|" + post.get("paytypeid") +
                "|" + post.get("site_orderid") +
                "|" + post.get("paymoney") +
                "|" + post.get("goodsname") +
                "|" + post.get("client_ip") +
                "|" + post.get("thereport_url") +
                "|" + post.get("thejump_url") +
                "|" + post.get("nowinttime") +
                "|" + payChannelConfig.getKey();
        post.put("sha1key",PayDigestUtil.digest(temp));

        String reqUrl = payChannelConfig.getReqUrl();
        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try{
            String req = parmObj.toJSONString();
            HttpPost httpPot = new HttpPost(reqUrl);
            _log.info("Itopay请求地址:{}", reqUrl);
            _log.info("Itopay请求数据:{}", req);
            StringEntity entityParams = new StringEntity(req, "utf-8");
            httpPot.setEntity(entityParams);
            client = HttpClients.createDefault();
            response = client.execute(httpPot);
            if(response != null && (response.getStatusLine().getStatusCode() -200 <100 )){
                HttpEntity entity = response.getEntity();
                String result =  EntityUtils.toString(entity);
                _log.info("Itopay请求结果:{}", result);
                JSONObject res =  JSONObject.parseObject(result);
                if ("200".equals(res.getString("code"))) {
                    int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null);
                    _log.info("[{}] Itopay 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                    JSONObject payInfo = new JSONObject();
                    payInfo.put("payUrl", res.getJSONObject("data").getShortValue("payinfo"));
                    payInfo.put("payMethod", PayConstant.PAY_METHOD_FORM_JUMP);
                    retObj.put("payParams", payInfo);
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
                    return retObj;
                }else {
                    retObj.put("errDes", "支付操作失败!");
                    retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
                    return retObj;
                }
            }else{
                _log.info("Itopay请求状态码response.getStatusLine().getStatusCode():{}", response.getStatusLine().getStatusCode());
                retObj.put("errDes", "支付操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }

        }catch (Exception e){
            retObj.put("errDes", "支付操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            return retObj;
        }finally {
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
     * 查询订单
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject query(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        ItopayConfig payChannelConfig = new ItopayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        retObj = buildRetObj(PayConstant.RETURN_VALUE_FAIL, "不支持的渠道[channelId=" + channelId + "]");
        return retObj;
    }


}
