package org.xxpay.pay.channel.dragonPay;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author: gf
 * @date: 2019-09-11 11:01:12
 * @description: DRAGONPAY配置
 */
@Component
public class DragonPayConfig {
    // 商户ID
    private String mchId;
    // 商户Key
    private String key;
    // 请求地址
    private String reqUrl;

    public DragonPayConfig(){}

    public DragonPayConfig(String payParam) {
        Assert.notNull(payParam, "init DragonPay config error");
        JSONObject object = JSONObject.parseObject(payParam);
        this.mchId = object.getString("mchId");
        this.key = object.getString("key");
        this.reqUrl = object.getString("reqUrl");
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String mchId) {
        this.mchId = mchId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getReqUrl() {
        return reqUrl;
    }

    public void setReqUrl(String reqUrl) {
        this.reqUrl = reqUrl;
    }
}
