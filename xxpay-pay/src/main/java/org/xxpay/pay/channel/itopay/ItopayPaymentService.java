package org.xxpay.pay.channel.itopay;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;


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
        String orderId = payOrder.getPayOrderId();

        Map<String, Object> post=new HashMap<String, Object>();
        post.put("site_orderid",orderId);//商户订单号
        post.put("paymoney", String.valueOf(payOrder.getAmount()));// 总金额,单位分
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

        String reqUrl = payChannelConfig.getReqUrl()+"/payp/interface/pay/payinit.php";
        HttpPost httpPost = new HttpPost(reqUrl);
        _log.info("Itopay支付请求地址:{}", reqUrl);
        _log.info("Itopay支付请求数据:{}", post.toString());

        //设置参数
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Iterator iterator = post.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,String> elem = (Map.Entry<String, String>) iterator.next();
            list.add(new BasicNameValuePair(elem.getKey(),String.valueOf(elem.getValue())));
        }
        if(list.size() > 0){
            UrlEncodedFormEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(list,"utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            httpPost.setEntity(entity);
        }

//        StringEntity entityParams = new StringEntity(post.toString(), "utf-8");
//        httpPot.setEntity(entityParams);

        //调用HTT请求函数
        JSONObject resObj = sendHttp(httpPost);
        if(resObj != null){
            /* 请求成功后对返回结果进行处理 */
            String channelOrderNo = resObj.getJSONObject("data").getString("trade_orderid");//渠道订单号
            int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), channelOrderNo,resObj.toJSONString());
            _log.info("[{}] Itopay 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
            JSONObject payInfo = new JSONObject();
            payInfo.put("payUrl", resObj.getJSONObject("data").getString("payinfo"));
            payInfo.put("payMethod", PayConstant.PAY_METHOD_FORM_JUMP);
            retObj.put("payParams", payInfo);
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
            return retObj;
        }else {
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
        String channelId = payOrder.getChannelId();
        ItopayConfig payChannelConfig = new ItopayConfig(getPayParam(payOrder));
        //返回值
        JSONObject retObj = new JSONObject();
        String orderId = payOrder.getPayOrderId();

        //发送参数拼装
        JSONObject post = new JSONObject();
        post.put("site_orderid",orderId);//商户订单号
        post.put("paymoney", String.valueOf(payOrder.getAmount()));// 总金额,单位分
        post.put("siteid", payChannelConfig.getMchId());//商户号
        post.put("nowinttime", System.currentTimeMillis()/1000);//时间戳
        String temp = post.get("siteid") +
                "|" + "" +
                "|" + post.get("site_orderid") +
                "|" + post.get("nowinttime") +
                "|" + payChannelConfig.getKey();
        post.put("sha1key",PayDigestUtil.digest(temp));
        //定义HTTP请求方式 POST、GET、PUT
        String reqUrl = payChannelConfig.getReqUrl()+"/payp/interface/pay/selorder.php";
        HttpPost httpPost = new HttpPost(reqUrl);
        _log.info("Itopay订单查询请求地址:{}", reqUrl);
        _log.info("Itopay订单查询请求数据:{}", post.toString());

        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Iterator iterator = post.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,String> elem = (Map.Entry<String, String>) iterator.next();
            list.add(new BasicNameValuePair(elem.getKey(),String.valueOf(elem.getValue())));
        }
        if(list.size() > 0){
            UrlEncodedFormEntity entity = null;
            try {
                entity = new UrlEncodedFormEntity(list,"utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            httpPost.setEntity(entity);
        }

        //调用HTT请求函数
        JSONObject resObj = sendHttp(httpPost);
        if(resObj != null){
            /* 请求成功后对返回结果进行处理 */
            String trade_state = resObj.getJSONObject("data").getString("status");
            retObj.put("obj", resObj);
            if(StrUtil.equals("SUCCESS",trade_state,true)){
                retObj.put("status", "2");//支付成功
            }else {
                retObj.put("status", "1");//支付中
            }
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
        }else{
            retObj.put("errDes", "查询操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
        }

        return retObj;
    }


    /**
     * 处理返回结果
     * @param response
     * @return
     */
    @Override
    protected JSONObject procRes(CloseableHttpResponse response) throws IOException {
        JSONObject res = null;
        if(response != null && (response.getStatusLine().getStatusCode() -200 <100 )){
            HttpEntity entity = response.getEntity();
            String temp =  EntityUtils.toString(entity);
            _log.info("Itopay请求结果:{}", temp);
            temp = temp.replace("[","{").replace("]","}");
            res =  JSONObject.parseObject(temp);
            if (StrUtil.equals("200",res.getString("code"),true)) {
                return res;
            }else {
                res = null;
            }
        }
        return res;
    }

}
