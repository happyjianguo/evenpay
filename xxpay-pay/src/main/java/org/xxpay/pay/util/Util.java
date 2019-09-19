package org.xxpay.pay.util;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.PayOrder;
import org.xxpay.pay.service.RpcCommonService;
import org.xxpay.pay.task.ReissuePayScheduled;

import java.util.Map;
import java.util.Random;

/**
 * @author: dingzhiwei
 * @date: 17/12/25
 * @description:
 */

@Component
public class Util {
    private static final MyLog _log = MyLog.getLog(Util.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public RpcCommonService rpcCommonService;

    public static boolean retIsSuccess(JSONObject retObj) {
        if(retObj == null) return false;
        return retObj.getBooleanValue(PayConstant.RETURN_PARAM_RETCODE);
    }

    // 构建威富通数据包
    public static String buildSwiftpayAttach(Map params) {
        JSONObject object = new JSONObject();
        object.put("bank_type", params.get("bank_type"));
        object.put("trade_type", params.get("trade_type"));
        return object.toString();
    }

    /**
     * 构建服务端跳转表单
     * @param jumpUrl
     * @param params
     * @return
     */
    public static String buildJumpForm(String jumpUrl, String params) {
        StringBuffer jumpForm = new StringBuffer();
        jumpForm.append("<form id=\"xxpay_jump\" name=\"xxpay_jump\" action=\""+jumpUrl+"\" method=\"post\">");
        jumpForm.append("<input type=\"hidden\" name=\"params\" id=\"params\" value=\""+params+"\">");
        jumpForm.append("<input type=\"submit\" value=\"立即支付\" style=\"display:none\" >");
        jumpForm.append("</form>");
        jumpForm.append("<script>document.forms[0].submit();</script>");
        return jumpForm.toString();
    }


    //是否需要扣量，根据商户ID查询redis变量值。mch.20000000 商户ID
    public  boolean isDeduction(PayOrder payOrder, String channelOrderNo) {
        // 判断redis中是否有扣量比例值
        String key = "mch."+payOrder.getMchId();
        if(stringRedisTemplate.hasKey(key)) {
            String value = stringRedisTemplate.opsForValue().get(key);
            _log.info("渠道{}扣量处理开始扣量比例为{}% payOrderId={}",payOrder.getMchId(),value,payOrder.getPayOrderId());
            int percentage =  Integer.parseInt(value);// 根据 redis 里面的值来决定命中百分比 0-100的数字为扣量百分比
            Random random = new Random();
            int i = random.nextInt(99);
            if(i>=0&&i<percentage) {
                //命中处理
                _log.info("扣量处理命中 payOrderId={}",payOrder.getPayOrderId());
                int updatePayOrderRows = rpcCommonService.rpcPayOrderService.updateStatus4Deduction(payOrder.getPayOrderId(),channelOrderNo,"");
                if (updatePayOrderRows == 1) {
                    _log.info("扣量数据库更新成功 payOrderId={}",payOrder.getPayOrderId());
                    return true;
                }else{
                    _log.info("扣量数据库更新失败 payOrderId={}",payOrder.getPayOrderId());
                }
            }else{
                _log.info("扣量处理未命中 payOrderId={}",payOrder.getPayOrderId());
            }
        }
        return false;
    }
}
