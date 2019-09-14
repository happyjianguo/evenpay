package org.xxpay.pay.channel.dragonpay;

import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.RefundOrder;
import org.xxpay.pay.channel.BaseRefund;

/**
 * @author: gf
 * @date: 2019-09-11 11:01:12
 * @description: DRAGONPAY退款
 */
@Service
public class DragonPayRefundService extends BaseRefund {

    private static final MyLog _log = MyLog.getLog(DragonPayRefundService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_DRAGONPAY;
    }

    public JSONObject refund(RefundOrder refundOrder) {
        DragonPayConfig payChannelConfig = new DragonPayConfig(getRefundParam(refundOrder));
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
        DragonPayConfig payChannelConfig = new DragonPayConfig(getRefundParam(refundOrder));
        JSONObject retObj = new JSONObject();
        
        return retObj;
    }
}
