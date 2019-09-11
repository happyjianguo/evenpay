package org.xxpay.pay.channel;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xxpay.core.common.Exception.ServiceException;
import org.xxpay.core.common.constant.MchConstant;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.constant.RetEnum;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.core.entity.PayPassageAccount;
import org.xxpay.pay.mq.BaseNotify4MchPay;
import org.xxpay.pay.service.RpcCommonService;
import org.xxpay.pay.util.Util;

/**
 * @author: dingzhiwei
 * @date: 17/12/24
 * @description:
 */
@Component
public abstract class BasePayNotify extends BaseService implements PayNotifyInterface {

    @Autowired
    public RpcCommonService rpcCommonService;

    @Autowired
    public PayConfig payConfig;

    @Autowired
    public BaseNotify4MchPay baseNotify4MchPay;

    public abstract String getChannelName();

    public JSONObject doNotify(Object notifyData) {
        return null;
    }

    public JSONObject doReturn(Object notifyData) {
        return new JSONObject();
    }

    /**
     * 获取三方支付配置信息
     * 如果是平台账户,则使用平台对应的配置,否则使用商户自己配置的渠道
     * @param payOrder
     * @return
     */
    public String getPayParam(PayOrder payOrder) {
        String payParam = "";
        PayPassageAccount payPassageAccount = rpcCommonService.rpcPayPassageAccountService.findById(payOrder.getPassageAccountId());
        if(payPassageAccount != null && payPassageAccount.getStatus() == MchConstant.PUB_YES) {
            payParam = payPassageAccount.getParam();
        }
        if(StringUtils.isBlank(payParam)) {
            throw new ServiceException(RetEnum.RET_MGR_PAY_PASSAGE_ACCOUNT_NOT_EXIST);
        }
        return payParam;
    }

    protected void notifyChannel(PayOrder payOrder,String transaction_id,JSONObject params) {
        // 处理订单
        byte payStatus = payOrder.getStatus(); // 0：订单生成，1：支付中，-1：支付失败，2：支付成功，3：业务处理完成，-2：订单过期
        //渠道交易码
        if (payStatus != PayConstant.PAY_STATUS_SUCCESS && payStatus != PayConstant.PAY_STATUS_COMPLETE) {
            int updatePayOrderRows = rpcCommonService.rpcPayOrderService.updateStatus4Success(payOrder.getPayOrderId(), transaction_id, params.toJSONString());
            if (updatePayOrderRows != 1) {
                _log.error("{}更新支付状态失败,将payOrderId={},更新payStatus={}失败", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
                retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                return retObj;
            }
            _log.error("{}更新支付状态成功,将payOrderId={},更新payStatus={}成功", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
            payOrder.setStatus(PayConstant.PAY_STATUS_SUCCESS);
        }
        baseNotify4MchPay.doNotify(payOrder, true);
        _log.info("====== 完成处理YMPAY支付回调通知 ======");
    }

}
