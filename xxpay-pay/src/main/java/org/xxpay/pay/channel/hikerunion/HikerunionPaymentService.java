package org.xxpay.pay.channel.hikerunion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.common.util.XXPayUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-08-01 10:05:17
 * @description: HIKERUNION支付接口
 */
@Service
public class HikerunionPaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(HikerunionPaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HIKERUNION;
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        HikerunionConfig payChannelConfig = new HikerunionConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();

        //对方只支持纯数字订单号
        String orderId = HikerunionUtil.getOrder(payOrder.getPayOrderId());
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("CLIENTREF",orderId); //商户订单号
        post.put("CLIENTKEY", payChannelConfig.getClientKey());
        post.put("AMOUNT", String.valueOf(payOrder.getAmount()));// 总金额,单位分
        post.put("CURRCODE","840");
        post.put("FUSEACTION", "main.cardEntry");
        post.put("LANGUAGECODE", "EN");
        post.put("REMARKS"," CategoryID:15");
        post.put("FAILURL",payOrder.getReturnUrl()+"?succeed=0");
        post.put("SUCCESSURL",payOrder.getReturnUrl()+"?succeed=1");
        post.put("VERSION", "1.0.0");
        //加签
        String key = payChannelConfig.getMchId()+"|"
                +post.get("CLIENTREF")+"|"
                +post.get("AMOUNT")+"|"
                +post.get("LANGUAGECODE")+"|"
                +post.get("CURRCODE")+"|"
                +post.get("VERSION")+"|"
                +PayDigestUtil.md5(payChannelConfig.getKey(),"utf-8");
        String temp = PayDigestUtil.md5(key,"utf-8");
        post.put("SIGNATURE", temp);

        String reqUrl = payChannelConfig.getReqUrl() + "?" + XXPayUtil.genUrlParams(post);
        _log.info("Hikerunion支付请求地址:{}", reqUrl);
        int resultDB = rpcCommonService.rpcPayOrderService.updateStatus4Ing(payOrder.getPayOrderId(), null,reqUrl);
        _log.info("[{}] Hikerunion 更新订单状态为支付中:payOrderId={},prepayId={},result={}", getChannelName(), payOrder.getPayOrderId(), "", resultDB);
        //支付链接返回给下级
        retObj.put("payUrl", reqUrl);
        retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);

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
        HikerunionConfig payChannelConfig = new HikerunionConfig(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        //对方只支持纯数字订单号位数小于等于18位
        String orderId = HikerunionUtil.getOrder(payOrder.getPayOrderId());

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
                //XmlUtils.toMap 字段名全部变小写
                Map<String,String> resultMap = XmlUtils.toMap(result.bodyBytes(),"utf-8");
                _log.info("Hikerunion查询订单返回数据:{}", resultMap);
                String resultFlag = resultMap.get("resultflag");
                //0: General failure, 1: Payment succeeded, 2: Payment failed, 3: Payment pending.
                if(StrUtil.equals("1",resultFlag,true)){
                    retObj.put("status", "2");//支付成功
                    String eirthref = resultMap.get("eirthref");
                    //上游渠道号更新
                    retObj.put("channelOrderNo", eirthref);
                }else {
                    retObj.put("status", "1");//支付中
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
