package org.xxpay.pay.channel.twsevenpay.util;

import org.xxpay.pay.channel.swiftpay.util.SignUtils;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.net.URLEncoder;

import org.xxpay.core.entity.PayOrder;
import java.io.UnsupportedEncodingException;
import cn.hutool.crypto.digest.DigestUtil;
import java.util.TimeZone;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DatePattern;
import java.util.stream.Collectors;

public class TwsevenpayUtil{
    public static final String PAY_URL = "";
    public static final String REFUND_URL = "";
    public static final String CANCEL_URL = "";

    public static HashMap<String,Object> buildPayParams(
            String mchId, String key, String orderId, PayOrder payOrder) {
        HashMap<String, Object> params = new HashMap<String, Object>();

        params.put("merchantnumber", mchId);
        params.put("ordernumber", orderId);
        params.put("amount", payOrder.getAmount());
        params.put("paymenttype", "creditcard");
        params.put("paytitle", payOrder.getSubject());
        params.put("payphone", payOrder.getParam1());
        params.put("consumerip", payOrder.getClientIp());

        // 默认只能交易台币
        params.put("currency", "TWD");
        // 默认交易模式为 N
        params.put("transmode", "N");

        String extra = payOrder.getExtra();
        JSONObject extraJSON = JSONUtil.parseObj(extra);
        params.put("payphone", extraJSON.getStr("payphone"));
        params.put("cardnumber", extraJSON.getStr("cardnumber"));
        params.put("cardexpiry", extraJSON.getStr("cardexpiry"));
        params.put("cardcvc2", extraJSON.getStr("cardcvc2"));

        // 添加台湾时区时间
        params.put("timestamp", getTimestamp());

        // 117pay 参数签名
        // map类型转换
        Map<String, String> map = params.entrySet().stream().filter(m -> m.getKey() != null && m.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> (String) e.getValue()));
        params.put("checksum", checksum(map, key));

        return params;
    }
    
    public static String buildPayURL(String url){
        return url + TwsevenpayUtil.PAY_URL;
    }

    public static String buildRefundURL(String url){
        return url + TwsevenpayUtil.REFUND_URL;
    }
    public static String buildCancelURL(String url){
        return url + TwsevenpayUtil.CANCEL_URL;
    }
    public static String getTimestamp(){
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Taipei");
        DateTime dt = DateTime.now();
        dt.setTimeZone(timeZone);
        return dt.toString(DatePattern.PURE_DATETIME_PATTERN);
    }

    public static String checksum(Map<String, String> params, String key) {
        StringBuilder buf = new StringBuilder((params.size() + 1) * 10);
        SignUtils.buildPayParams(buf, params, false);
        String preStr = buf.toString() + key;
        return DigestUtil.sha256Hex(getContentBytes(preStr, "utf8")).toUpperCase();
    }

    private static byte[] getContentBytes(String content, String charset) {
        if (charset == null || "".equals(charset)) {
            return content.getBytes();
        }
        try {
            return content.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("MD5签名过程中出现错误,指定的编码集不对,您目前指定的编码集是:" + charset);
        }
    }

    public static String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Throwable e) {
            return str;
        }
    }
}
