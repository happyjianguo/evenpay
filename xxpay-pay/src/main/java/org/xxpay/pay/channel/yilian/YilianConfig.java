package org.xxpay.pay.channel.yilian;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author: gf
 * @date: 2019-08-14 19:35:44
 * @description: YILIAN配置
 */
@Component
public class YilianConfig {
    // 商户ID
    private String mchId;
    // 商户Key
    private String key;
    // 请求地址
    private String reqUrl;

    // pfx 文件地址
    private String pfxUrl;

    // pfx 密码
    private String pfxPass;

    //后台登录名
    private String userName;

    public YilianConfig(){}

    public YilianConfig(String payParam) {
        Assert.notNull(payParam, "init Yilian config error");
        JSONObject object = JSONObject.parseObject(payParam);
        this.mchId = object.getString("mchId");
        this.key = object.getString("key");
        this.reqUrl = object.getString("reqUrl");
        this.pfxUrl = object.getString("pfxUrl");
        this.pfxPass = object.getString("pfxPass");
        this.userName = object.getString("userName");
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

    public void setPfxUrl(String pfxUrl) {
        this.pfxUrl = pfxUrl;
    }

    public String getPfxUrl() {
        return pfxUrl;
    }

    public String getPfxPass() {
        return pfxPass;
    }

    public void setPfxPass(String pfxPass) {
        this.pfxPass = pfxPass;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

}
