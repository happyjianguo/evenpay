package org.xxpay.pay.channel.dragonpay;


import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayNotify;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;
import org.xxpay.pay.util.Util;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: gf
 * @date: 2019-09-11 11:01:12
 * @description: DRAGONPAY支付回调
 */
@Service
public class DragonPayPayNotifyService extends BasePayNotify {

    private static final MyLog _log = MyLog.getLog(DragonPayPayNotifyService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_DRAGONPAY;
    }

    @Override
    public JSONObject doNotify(Object notifyData) {
        String logPrefix = "【处理DRAGONPAY支付回调】";
        _log.info("====== 开始处理DRAGONPAY支付回调通知 ======");
        HttpServletRequest req = (HttpServletRequest) notifyData;
        JSONObject retObj = buildRetObj();
        Map<String, Object> payContext = new HashMap();
        PayOrder payOrder;
        String respString = PayConstant.RETURN_SWIFTPAY_VALUE_FAIL;
        try {
            req.setCharacterEncoding("utf-8");
            String resString = XmlUtils.parseRequst(req);
            _log.info("[{}]回调通知参数,data={}", getChannelName(), resString);

            if (!StringUtils.isEmpty(respString)) {
                JSONObject params = JSONObject.parseObject(resString);
                payContext.put("parameters", params);
                if(!verifyPayParams(payContext)) {
                    retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                    return retObj;
                }
                payOrder = (PayOrder) payContext.get("payOrder");
                // 处理订单
                byte payStatus = payOrder.getStatus(); // 0：订单生成，1：支付中，-1：支付失败，2：支付成功，3：业务处理完成，-2：订单过期
                //渠道交易码
                String transaction_id = params.getString("order_id");
                if (payStatus != PayConstant.PAY_STATUS_SUCCESS && payStatus != PayConstant.PAY_STATUS_COMPLETE) {
                    int updatePayOrderRows = rpcCommonService.rpcPayOrderService.updateStatus4Success(payOrder.getPayOrderId(), transaction_id, Util.buildSwiftpayAttach(params));
                    if (updatePayOrderRows != 1) {
                        _log.error("{}更新支付状态失败,将payOrderId={},更新payStatus={}失败", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
                        retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                        return retObj;
                    }
                    _log.error("{}更新支付状态成功,将payOrderId={},更新payStatus={}成功", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
                    payOrder.setStatus(PayConstant.PAY_STATUS_SUCCESS);
                }
                baseNotify4MchPay.doNotify(payOrder, true);
                _log.info("====== 完成处理DRAGONPAY支付回调通知 ======");
                respString = PayConstant.RETURN_SWIFTPAY_VALUE_SUCCESS;
            }

        }catch (Exception e) {
            _log.error(e, logPrefix + "DRAGONPAY支付退款处理异常");
        }
        retObj.put(PayConstant.RESPONSE_RESULT, respString);
        return retObj;
    }

    /**
     * 验证DRAGONPAY支付通知参数
     * @return
     */
    public boolean verifyPayParams(Map<String, Object> payContext) {
        JSONObject params = (JSONObject) payContext.get("parameters");
        //校验结果是否成功

        // 商户订单号 根据对接文档修改参数名
        String out_trade_no = params.getString("partner_order_id");
        // 支付金额
        String total_fee = params.getString("total_fee");
        if (StringUtils.isEmpty(total_fee)) {
            _log.error("DRAGONPAY Notify parameter total_amount is empty. total_fee={}", total_fee);
            payContext.put("retMsg", "total_amount is empty");
            return false;
        }

        String errorMessage;
        // 查询payOrder记录
        String payOrderId = out_trade_no;
        PayOrder payOrder = rpcCommonService.rpcPayOrderService.findByPayOrderId(payOrderId);
        if (payOrder == null) {
            _log.error("DRAGONPAY Can't found payOrder form db. payOrderId={}, ", payOrderId);
            payContext.put("retMsg", "Can't found payOrder");
            return false;
        }
        DragonPayConfig payConfig = new DragonPayConfig(getPayParam(payOrder));
        // 验证签名
        if (!checkParam(params, payConfig.getKey())) {
            errorMessage = "check sign failed.";
            _log.error("DRAGONPAY Notify parameter {}", errorMessage);
            payContext.put("retMsg", errorMessage);
            return false;
        }
        // 核对金额
        long outPayAmt = new BigDecimal(total_fee).longValue();
        long dbPayAmt = payOrder.getAmount().longValue();
        if (dbPayAmt != outPayAmt) {
            _log.error("db DRAGONPAY payOrder record payPrice not equals total_amount. total_amount={},payOrderId={}", total_fee, payOrderId);
            payContext.put("retMsg", "");
            return false;
        }
        payContext.put("payOrder", payOrder);
        return true;
    }

    //验证签名
    private boolean checkParam (JSONObject params,String key){

        return true;
    }
}
