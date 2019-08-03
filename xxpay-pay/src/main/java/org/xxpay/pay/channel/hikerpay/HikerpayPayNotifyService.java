package org.xxpay.pay.channel.hikerpay;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayNotify;
import org.xxpay.pay.channel.hikerpay.util.HikerUtil;
import org.xxpay.pay.channel.swiftpay.util.JsonUtil;
import org.xxpay.pay.channel.swiftpay.util.SignUtils;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;
import org.xxpay.pay.util.Util;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;


/**
 * @author: gf
 * @date: 2018/07/19
 * @description: 海科支付回调
 */
@Service
public class HikerpayPayNotifyService extends BasePayNotify {

    private static final MyLog _log = MyLog.getLog(HikerpayPayNotifyService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HKPAY;
    }

    @Override
    public JSONObject doNotify(Object notifyData) {
        String logPrefix = "【处理海科支付回调】";
        _log.info("====== 开始处理海科支付回调通知 ======");
        HttpServletRequest req = (HttpServletRequest) notifyData;
        JSONObject retObj = buildRetObj();
        Map<String, Object> payContext = new HashMap();
        PayOrder payOrder;
        String respString = PayConstant.RETURN_SWIFTPAY_VALUE_FAIL;
        try {
            req.setCharacterEncoding("utf-8");
            String resString = XmlUtils.parseRequst(req);
            _log.info("[{}]回调通知参数,data={}", getChannelName(), resString);


            if (!StringUtils.isEmpty(resString)) {
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
                _log.info("====== 完成处理海科支付回调通知 ======");
                respString = PayConstant.RETURN_SWIFTPAY_VALUE_SUCCESS;
            }

        }catch (Exception e) {
            _log.error(e, logPrefix + "海科支付回调处理异常");
        }
        retObj.put(PayConstant.RESPONSE_RESULT, respString);
        return retObj;
    }

    /**
     * 验证海科支付通知参数
     * @return
     */
    public boolean verifyPayParams(Map<String, Object> payContext) {
        JSONObject params = (JSONObject) payContext.get("parameters");
        //校验结果是否成功
        String time = params.getString("time");
        String nonce_str = params.getString("nonce_str");
        String sign = params.getString("sign");
        if (StringUtils.isEmpty(time) || StringUtils.isEmpty(nonce_str) || StringUtils.isEmpty(sign)) {
            _log.error("time={},nonce_str={},sign={}", time, nonce_str, sign);
            payContext.put("retMsg", "hiker notify data failed");
            return false;
        }
        // 商户订单号
        String out_trade_no = params.getString("partner_order_id");
        // 支付金额
        String total_fee = params.getString("total_fee");
        if (StringUtils.isEmpty(total_fee)) {
            _log.error("Hiker Notify parameter total_amount is empty. total_fee={}", total_fee);
            payContext.put("retMsg", "total_amount is empty");
            return false;
        }

        String errorMessage;
        // 查询payOrder记录
        String payOrderId = out_trade_no;
        PayOrder payOrder = rpcCommonService.rpcPayOrderService.findByPayOrderId(payOrderId);
        if (payOrder == null) {
            _log.error("HikerPay Can't found payOrder form db. payOrderId={}, ", payOrderId);
            payContext.put("retMsg", "Can't found payOrder");
            return false;
        }
        HikerpayConfig hikertpayConfig = new HikerpayConfig(getPayParam(payOrder));
        String CREDENTIAL_CODE = hikertpayConfig.getKey();
        String PARTNER_CODE = hikertpayConfig.getMchId();
        // 验证签名

        if (!checkParam(params,PARTNER_CODE,CREDENTIAL_CODE)) {
            errorMessage = "check sign failed.";
            _log.error("Swiftpay Notify parameter {}", errorMessage);
            payContext.put("retMsg", errorMessage);
            return false;
        }
        // 核对金额
        long outPayAmt = new BigDecimal(total_fee).longValue();
        long dbPayAmt = payOrder.getAmount().longValue();
        if (dbPayAmt != outPayAmt) {
            _log.error("db haiker payOrder record payPrice not equals total_amount. total_amount={},payOrderId={}", total_fee, payOrderId);
            payContext.put("retMsg", "");
            return false;
        }
        payContext.put("payOrder", payOrder);
        return true;
    }

    //验证签名
    public static boolean checkParam(JSONObject params,String PARTNER_CODE,String CREDENTIAL_CODE) {
        //long time = System.currentTimeMillis();
        boolean result = false;
        String validStr = PARTNER_CODE + "&" + params.getString("time") + "&" + params.getString("nonce_str") + "&" + CREDENTIAL_CODE;
        String signV = DigestUtils.sha256Hex(validStr).toLowerCase();
        String sign = params.getString("sign");
        if(!StringUtils.isEmpty(sign) && sign.equalsIgnoreCase(signV)) {
            result = true;
        }
        return result;
    }


}
