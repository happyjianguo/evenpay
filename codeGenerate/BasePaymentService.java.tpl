package ${PACKAGE_NAME};

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
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayment;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;
import java.io.IOException;


/**
 * @author: gf
 * @date: ${DATA}
 * @description: ${CHANNEL_NAME}支付接口
 */
@Service
public class ${CLASS_NAME}PaymentService extends BasePayment {
    private static final MyLog _log = MyLog.getLog(${CLASS_NAME}PaymentService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_${CHANNEL_NAME};
    }


    /**
     * 支付
     * @param payOrder
     * @return
     */
    @Override
    public JSONObject pay(PayOrder payOrder) {
        String channelId = payOrder.getChannelId();
        ${CLASS_NAME}Config payConfig = new ${CLASS_NAME}Config(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        JSONObject parmObj = new JSONObject();

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
        ${CLASS_NAME}Config payConfig = new ${CLASS_NAME}Config(getPayParam(payOrder));
        JSONObject retObj = new JSONObject();
        String orderId = payOrder.getPayOrderId();
        
        return retObj;
    }


}
