package ${PACKAGE_NAME};

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author: gf
 * @date: ${DATA}
 * @description: ${CHANNEL_NAME}配置
 */
@Component
public class ${CLASS_NAME}Config {
    // 商户ID
    private String mchId;
    // 商户Key
    private String key;
    // 请求地址
    private String reqUrl;

    public ${CLASS_NAME}Config(){}

    public ${CLASS_NAME}Config(String payParam) {
        Assert.notNull(payParam, "init ${CLASS_NAME} config error");
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
