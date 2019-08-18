package org.xxpay.pay.channel.itopay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.common.util.PayDigestUtil;
import org.xxpay.core.entity.TransOrder;
import org.xxpay.pay.channel.BaseTrans;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: gf
 * @date: 2019-08-14 19:35:44
 * @description: YILIAN代付接口
 */
@Service
public class ItopayTransService extends BaseTrans {
    private static final MyLog _log = MyLog.getLog(ItopayTransService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_ITOPAY;
    }

    public JSONObject trans(TransOrder transOrder) {
        ItopayConfig payChannelConfig = new ItopayConfig(getTransParam(transOrder));
        String logPrefix = "【全时新转账(代付)】";
        JSONObject retObj = buildRetObj();
        Map<String, Object> post=new HashMap<String, Object>();
        _log.info("代付 transOrder {}",transOrder.toString());
        post.put("bankid","auto");
        post.put("siteid", payChannelConfig.getMchId());//商户号
        post.put("siteorderid", transOrder.getTransOrderId());
        post.put("payee_name", transOrder.getAccountName());//收款方账户名或姓名
        post.put("payee_account", transOrder.getAccountNo());//收款方银行账户卡号
//      1收款方支付-付款金额减去手续费(实际到账少2元)，实际扣款为:提交代付金额
//      2付款方支付-单独减去手续费，实际扣款为:提交代付金额+2元
        post.put("sxftype","2");//手续费支付方【只能填1或2】
        post.put("payamount",transOrder.getAmount());
        post.put("bank_name",transOrder.getBankName());
        post.put("bank_branche",transOrder.getExtra());//收款人账户对应开户行支行名称，请填写完整银行名称，列如北京招商银行小关支行
        post.put("reporturl",payConfig.getNotifyTransUrl(getChannelName()));
        post.put("nowinttime", System.currentTimeMillis()/1000);//时间戳
//      收款方类型【只能填1或2】1为个人对私，2为企业对公
        post.put("payee_type",transOrder.getAccountAttr()+1);
        post.put("thenote",transOrder.getRemarkInfo());
        post.put("bank_province",transOrder.getProvince());
        post.put("bank_city",transOrder.getCity());

        String temp = post.get("bankid") +
                "|" + post.get("siteid") +
                "|" + post.get("siteorderid") +
                "|" + post.get("payee_name") +
                "|" + post.get("payee_account") +
                "|" + post.get("sxftype") +
                "|" + post.get("payamount") +
                "|" + post.get("bank_name") +
                "|" + post.get("bank_branche") +
                "|" + post.get("reporturl") +
                "|" + post.get("nowinttime") +
                "|" + payChannelConfig.getKey();
        post.put("sha1key", PayDigestUtil.digest(temp));
        String reqUrl = payChannelConfig.getReqUrl()+"/payd/interface/dinit.php";
        _log.info("Itopay代付请求地址:{}", reqUrl);
        _log.info("Itopay代付请求数据:{}", post.toString());
        try{
            HttpResponse result = HttpRequest.post(reqUrl)
                    .form(post)
                    .execute();
            _log.info("Itopay代付请求返回:{}",result.body());
            if (result.isOk()) {
                String body = result.body().replace("[","{").replace("]","}");
                JSONObject json = JSONObject.parseObject(body);
                if (StrUtil.equals("200",json.getString("code"))) {
                    retObj.put("isSuccess", true);
                    retObj.put("status", 2);    // 成功
                    //更新渠道代付单号
                    retObj.put("channelOrderNo", json.getJSONObject("data").getString("myorderid"));
                }else{
                    //出现业务错误
                    _log.info("{}返回失败", logPrefix);
                    _log.info("sub_code:{} sub_msg:{}}", json.getString("code"),json.getString("msg"));
                    retObj.put("status", 3);    // 失败
                    retObj.put("channelErrCode", json.getString("code"));
                    retObj.put("channelErrMsg", json.getString("msg"));
                }
            }else{
                retObj = buildFailRetObj();
            }
        } catch (Exception e) {
            _log.error(e, "");
            retObj = buildFailRetObj();
        }
        return  retObj;
    }

    public JSONObject query(TransOrder transOrder) {
        ItopayConfig payChannelConfig = new ItopayConfig(getTransParam(transOrder));
        String logPrefix = "【全时新转账(代付)查询】";
        JSONObject retObj = buildRetObj();
        Map<String, Object> post=new HashMap<String, Object>();
        post.put("siteid", payChannelConfig.getMchId());//商户号
        post.put("siteorderid",transOrder.getTransOrderId());
        post.put("nowinttime", System.currentTimeMillis()/1000);//时间戳
        String temp = post.get("siteid") +
                "|" + post.get("siteorderid") +
                "|" + post.get("nowinttime") +
                "|" + payChannelConfig.getKey();
        post.put("sha1key", PayDigestUtil.digest(temp));
        String reqUrl = payChannelConfig.getReqUrl()+"/payd/interface/dinit.php";
        _log.info("Itopay代付查询请求地址:{}", reqUrl);
        _log.info("Itopay代付查询请求数据:{}", post.toString());
        try{
            HttpResponse result = HttpRequest.post(reqUrl)
                    .form(post)
                    .execute();
            _log.info("Itopay代付查询请求返回:{}",result.body());
            if (result.isOk()) {
                String body = result.body().replace("[","{").replace("]","}");
                JSONObject json = JSONObject.parseObject(body);
                if (StrUtil.equals("200",json.getString("code"))) {
                    String trade_state = json.getJSONObject("data").getString("datastat");
                    retObj.put("obj", json);
//                  代付最终状态标识（成功失败状态值）
//                  SUCCESS为成功
//                  FAIL为失败
//                  NONE为状态未知，代付处理中
//                  TUIKUAN（极为少见的错误填写信息导致卡单进而出现的特殊状态，一般需人工确认，即响应付款成功后但实际未到账，由银行人工最终确认后做退款处理的标识状态）
                    if(StrUtil.equals("SUCCESS",trade_state,true)) {
                        retObj.put("isSuccess", true);
                    }else{
                        retObj.put("isSuccess", false);
                    }
                }else{
                    //出现业务错误
                    _log.info("{}返回失败", logPrefix);
                    _log.info("sub_code:{} sub_msg:{}}", json.getString("code"),json.getString("msg"));
                    retObj.put("channelErrCode", json.getString("code"));
                    retObj.put("channelErrMsg", json.getString("msg"));
                }
            }else{
                retObj = buildFailRetObj();
            }
        }catch (Exception e) {
            _log.error(e, "");
            retObj = buildFailRetObj();
        }

        return retObj;
    }

    @Override
    public JSONObject balance(String payParam) {
        return null;
    }
}
