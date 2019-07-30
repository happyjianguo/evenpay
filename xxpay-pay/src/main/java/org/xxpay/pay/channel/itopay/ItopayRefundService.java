package org.xxpay.pay.channel.itopay;

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
 * @date: 2019-07-29 17:42:31
 * @description: ITOPAY退款
 */
@Service
public class ItopayRefundService extends BaseRefund {

    private static final MyLog _log = MyLog.getLog(ItopayRefundService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_ITOPAY;
    }

    public JSONObject refund(RefundOrder refundOrder) {
        JSONObject retObj = buildRetObj(PayConstant.RETURN_VALUE_FAIL, "不支持退款的渠道[channelId=" + refundOrder.getChannelId() + "]");
        return retObj;
    }


    /**
     * 查询退款订单
     * @param refundOrder
     * @return
     */
    @Override
    public JSONObject query(RefundOrder refundOrder) {
        JSONObject retObj = buildRetObj(PayConstant.RETURN_VALUE_FAIL, "不支持退款的渠道[channelId=" + refundOrder.getChannelId() + "]");
        return retObj;
    }
}
