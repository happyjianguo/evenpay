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
import org.xxpay.core.entity.RefundOrder;
import org.xxpay.pay.channel.BaseRefund;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;

import java.io.IOException;

/**
 * @author: gf
 * @date: ${DATA}
 * @description: ${CHANNEL_NAME}退款
 */
@Service
public class ${CLASS_NAME}RefundService extends BaseRefund {

    private static final MyLog _log = MyLog.getLog(${CLASS_NAME}RefundService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_${CHANNEL_NAME};
    }

    public JSONObject refund(RefundOrder refundOrder) {
        ${CLASS_NAME}Config payConfig = new ${CLASS_NAME}Config(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();
        JSONObject param = new JSONObject();
        
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
        ${CLASS_NAME}Config payConfig = new ${CLASS_NAME}Config(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();
        
        return retObj;
    }
}
