package org.xxpay.pay.channel.hikerunion;


import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.channel.BasePayNotify;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;
import org.xxpay.pay.util.Util;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;


/**
 * @author: gf
 * @date: 2019-08-01 10:05:17
 * @description: HIKERUNION支付回调
 */
@Service
public class HikerunionPayNotifyService extends BasePayNotify {

    private static final MyLog _log = MyLog.getLog(HikerunionPayNotifyService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_HIKERUNION;
    }

    @Override
    public JSONObject doNotify(Object notifyData) {
        String logPrefix = "【处理HIKERUNION支付回调】";
        _log.info("====== 开始处理HIKERUNION支付回调通知 ======");
        HttpServletRequest req = (HttpServletRequest) notifyData;
        JSONObject retObj = buildRetObj();
        Map<String, Object> payContext = new HashMap();
        PayOrder payOrder;
        String respString = PayConstant.RETURN_SWIFTPAY_VALUE_FAIL;
        try {
            req.setCharacterEncoding("utf-8");
            SortedMap temp = XmlUtils.getParameterMap(req);
            _log.info("[{}]回调通知参数,data={}", getChannelName(), temp);

            if (!StringUtils.isEmpty(temp.get("resultflag"))) {
                JSONObject params = new JSONObject(temp);
                payContext.put("parameters", params);
                if(!verifyPayParams(payContext)) {
                    retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                    return retObj;
                }
                payOrder = (PayOrder) payContext.get("payOrder");
                // 处理订单
                byte payStatus = payOrder.getStatus(); // 0：订单生成，1：支付中，-1：支付失败，2：支付成功，3：业务处理完成，-2：订单过期
                //渠道交易码
                String transaction_id = params.getString("eirthref");
                if (payStatus != PayConstant.PAY_STATUS_SUCCESS && payStatus != PayConstant.PAY_STATUS_COMPLETE) {
                    int updatePayOrderRows = rpcCommonService.rpcPayOrderService.updateStatus4Success(payOrder.getPayOrderId(), transaction_id, params.toString());
                    if (updatePayOrderRows != 1) {
                        _log.error("{}更新支付状态失败,将payOrderId={},更新payStatus={}失败", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
                        retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                        return retObj;
                    }
                    _log.error("{}更新支付状态成功,将payOrderId={},更新payStatus={}成功", logPrefix, payOrder.getPayOrderId(), PayConstant.PAY_STATUS_SUCCESS);
                    payOrder.setStatus(PayConstant.PAY_STATUS_SUCCESS);
                }
                baseNotify4MchPay.doNotify(payOrder, true);
                _log.info("====== 完成处理HIKERUNION支付回调通知 ======");
                respString = PayConstant.RETURN_SWIFTPAY_VALUE_SUCCESS;
            }

        }catch (Exception e) {
            _log.error(e, logPrefix + "HIKERUNION支付退款处理异常");
        }
        retObj.put(PayConstant.RESPONSE_RESULT, respString);
        return retObj;
    }

    /**
     * 验证HIKERUNION支付通知参数
     * @return
     */
    public boolean verifyPayParams(Map<String, Object> payContext) {
        Map<String,String> params = (Map<String,String>) payContext.get("parameters");
        //校验结果是否成功

        // 商户订单号 根据对接文档修改参数名
        String out_trade_no = HikerunionUtil.reduOrder(params.get("clientRef"));
        // 支付金额
        String total_fee = params.get("amount");
        if (StringUtils.isEmpty(total_fee)) {
            _log.error("HIKERUNION Notify parameter total_amount is empty. total_fee={}", total_fee);
            payContext.put("retMsg", "total_amount is empty");
            return false;
        }

        String errorMessage;
        // 查询payOrder记录
        String payOrderId = out_trade_no;
        PayOrder payOrder = rpcCommonService.rpcPayOrderService.findByPayOrderId(payOrderId);
        if (payOrder == null) {
            _log.error("HIKERUNION Can't found payOrder form db. payOrderId={}, ", payOrderId);
            payContext.put("retMsg", "Can't found payOrder");
            return false;
        }
        HikerunionConfig payChannelConfig = new HikerunionConfig(getPayParam(payOrder));
        //放入商户号
        params.put("CLIENTMID",payChannelConfig.getMchId());
        // 验证签名
        if (!checkParam(params, payChannelConfig.getKey())) {
            errorMessage = "check sign failed.";
            _log.error("HIKERUNION Notify parameter {}", errorMessage);
            payContext.put("retMsg", errorMessage);
            return false;
        }
        // 核对金额
        long outPayAmt = new BigDecimal(total_fee).longValue();
        long dbPayAmt = payOrder.getAmount().longValue();
        if (dbPayAmt != outPayAmt) {
            _log.error("db HIKERUNION payOrder record payPrice not equals total_amount. total_amount={},payOrderId={}", total_fee, payOrderId);
            payContext.put("retMsg", "");
            return false;
        }
        payContext.put("payOrder", payOrder);
        return true;
    }

    //验证签名
    private boolean checkParam (Map<String,String> params,String key){
        boolean result = false;
        String temp = PayDigestUtil.md5(params.get("CLIENTMID") +
                "|" + params.get("clientRef") +
                "|" + params.get("eirthRef") +
                "|" + params.get("resultflag") +
                "|" + params.get("amount") +
                "|" + params.get("currCode") +
                "|" + PayDigestUtil.md5(key,"utf-8"),"uft-8");
        String sign = params.get("secureHash");
        if (StrUtil.equals(temp,sign,true)) {
            result = true;
        }else{
            _log.error("HIKERUNION 支付回调验签不通过");
        }
        return result;
    }
}
