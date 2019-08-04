package org.xxpay.pay.channel.itopay;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.StringUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.TransOrder;
import org.xxpay.pay.channel.BaseTransNotify;
import org.xxpay.pay.channel.swiftpay.util.XmlUtils;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author: gf
 * @date: 2019-08-04 16:52:31
 * @description: ITOPAY转账回调
 */
public class ItopayTransNotifyService extends BaseTransNotify {
    private static final MyLog _log = MyLog.getLog(ItopayTransNotifyService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_ITOPAY;
    }

    @Override
    public JSONObject doNotify(Object notifyData) {
        String logPrefix = "【处理ITOPAY转账回调】";
        _log.info("====== 开始处理ITOPAY转账回调通知 ======");
        HttpServletRequest req = (HttpServletRequest) notifyData;
        JSONObject retObj = buildRetObj();
        Map<String, Object> payContext = new HashMap();
        TransOrder transOrder;
        String respString = PayConstant.RETURN_SWIFTPAY_VALUE_FAIL;
        try {
            req.setCharacterEncoding("utf-8");
            SortedMap temp = XmlUtils.getParameterMap(req);
            _log.info("[{}]转账回调通知参数,data={}", getChannelName(), temp);
            if (StrUtil.equals("SUCCESS",(String)temp.get("datastat"))) {
                JSONObject params = new JSONObject(temp);
                payContext.put("parameters", params);
                if(!verifyPayParams(payContext)) {
                    retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                    return retObj;
                }
                transOrder = (TransOrder) payContext.get("transOrder");
                //转账状态:0-订单生成,1-转账中,2-转账成功,3-转账失败,4-业务处理完成
                byte transStatus = transOrder.getStatus();
                //渠道交易码
                String transaction_id = params.getString("myorderid");
                if (transStatus != PayConstant.TRANS_STATUS_SUCCESS && transStatus != PayConstant.TRANS_STATUS_COMPLETE) {
                    int updatePayOrderRows = rpcCommonService.rpcTransOrderService.updateStatus4Success(transOrder.getTransOrderId(),transaction_id);
                    if (updatePayOrderRows != 1) {
                        _log.error("{}更新支付状态失败,将transOrderId={},更新transStatus={}失败", logPrefix, transOrder.getTransOrderId(), PayConstant.TRANS_STATUS_SUCCESS);
                        retObj.put(PayConstant.RESPONSE_RESULT, PayConstant.RETURN_SWIFTPAY_VALUE_FAIL);
                        return retObj;
                    }
                    _log.error("{}更新支付状态成功,将transOrderId={},更新transStatus={}成功", logPrefix, transOrder.getTransOrderId(), PayConstant.TRANS_STATUS_SUCCESS);
                    transOrder.setStatus(PayConstant.TRANS_STATUS_SUCCESS);
                }
                baseNotify4MchTrans.doNotify(transOrder, true);
                _log.info("====== 完成处理ITOPAY转账回调通知 ======");
                respString = PayConstant.RETURN_SWIFTPAY_VALUE_SUCCESS;
            }
        }catch (Exception e) {
            _log.error(e, logPrefix + "ITOPAY转账回调处理异常");
        }
        retObj.put(PayConstant.RESPONSE_RESULT, respString);
        return retObj;
    }


    /**
     * 验证ITOPAY支付通知参数
     * @return
     */
    public boolean verifyPayParams(Map<String, Object> payContext) {
        JSONObject params = (JSONObject) payContext.get("parameters");
        //校验结果是否成功

        // 商户订单号 根据对接文档修改参数名
        String out_trade_no = params.getString("siteorderid");

        String errorMessage;
        // 查询payOrder记录
        String transOrderId = out_trade_no;

        TransOrder transOrder = rpcCommonService.rpcTransOrderService.findByTransOrderId(transOrderId);
        if (transOrder == null) {
            _log.error("ITOPAY Can't found transOrder form db. transOrderId={}, ", transOrderId);
            payContext.put("retMsg", "Can't found transOrder");
            return false;
        }
        ItopayConfig payConfig = new ItopayConfig(getTransParam(transOrder));
        // 验证签名
        if (!checkParam(params, payConfig.getKey())) {
            errorMessage = "check sign failed.";
            _log.error("ITOPAY Notify parameter {}", errorMessage);
            payContext.put("retMsg", errorMessage);
            return false;
        }
        payContext.put("transOrder", transOrder);
        return true;
    }

    //验证签名
    public static boolean checkParam(JSONObject params,String key) {
        boolean result = false;
        String temp = params.get("siteid") +
                "|" + params.get("bankid") +
                "|" + params.get("datastat") +
                "|" + params.get("hyflag") +
                "|" + params.get("yuanstat") +
                "|" + params.get("myorderid") +
                "|" + params.get("siteorderid") +
                "|" + key;
        String signV = PayDigestUtil.digest(temp);
        String sign = params.getString("sha1key");
        if(!StringUtils.isEmpty(sign) && sign.equalsIgnoreCase(signV)) {
            result = true;
        }
        return result;
    }
}
