package org.xxpay.pay.channel.doupay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-09-12 19:06:15
 * @description: DOUPAY支付接口
 */
@Service
public class DoupayPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(DoupayPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_DOUPAY;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        DoupayConfig payChannelConfig = new DoupayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("merchant_code",payChannelConfig.getMchId());
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time=format.format(new Date());
        post.put("order_no", payOrder.getPayOrderId());
        post.put("interface_version","V3.1");
        post.put("order_amount", (payOrder.getAmount()/100));
        post.put("notify_url", payConfig.getNotifyUrl(getChannelName()));
        post.put("service_type", "alipay_h5api");
        post.put("order_time", time);
        post.put("product_name", "test");
        post.put("client_ip", "222.240.116.55");
        try {
            String sign = PayDigestUtil.getRSASign(post,payChannelConfig.getKey());
            post.put("sign_type", "RSA-S");
            post.put("sign", sign);
            HttpResponse result = HttpRequest.post(payChannelConfig.getReqUrl()+"/gateway/api/h5apipay")
                    .form(post)
                    .execute();
            if (result.isOk()) {
                //XmlUtils.toMap 字段名全部变小写
                String body = result.body().replace("<dinpay>","")
                        .replace("</dinpay>","");
                Map<String,String> resultMap = XmlUtils.toMap(body.getBytes(Charset.forName("UTF-8")),"utf-8");
                _log.info("DouPay 支付请求返回数据:{}", resultMap);

                if (StrUtil.equals(resultMap.get("result_code"),"0")) {
                    int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null);
                    _log.info("[{}] DouPay 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
                    JSONObject payInfo = new JSONObject();
                    String payurl = resultMap.get("payurl");
                    payInfo.put("payUrl", URLDecoder.decode(payurl,"UTF-8" ));
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
        DoupayConfig payChannelConfig = new DoupayConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        String orderId = payOrder.getPayOrderId();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("merchant_code",payChannelConfig.getMchId());
        post.put("order_no", payOrder.getPayOrderId());
        post.put("interface_version","V3.1");
        post.put("service_type", "alipay_h5api");
        try {
            String sign = PayDigestUtil.getRSASign(post,payChannelConfig.getKey());
            post.put("sign_type", "RSA-S");
            post.put("sign", sign);
            HttpResponse result = HttpRequest.post("http://47.244.186.46:8081/query")
                    .form(post)
                    .execute();
            if (result.isOk()) {
                String body = result.body().replace("<dinpay>","")
                        .replace("</dinpay>","");
                Map<String,String> resultMap = XmlUtils.toMap(body.getBytes(Charset.forName("UTF-8")),"utf-8");
                _log.info("DouPay 查询订单返回数据:{}", resultMap);
                if (StrUtil.equals(resultMap.get("trade_status"),"SUCCESS")) {
                    retObj.put("status", "2");//支付成功
                    String eirthref = resultMap.get("trade_no");
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
        }catch (Exception e){
            _log.error(e.getMessage());
            retObj.put("errDes", "查询操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            return retObj;
        }
        return retObj;
    }


}
