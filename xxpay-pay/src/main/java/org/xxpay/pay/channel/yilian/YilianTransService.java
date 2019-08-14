package org.xxpay.pay.channel.yilian;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;
import org.xxpay.core.entity.TransOrder;
import org.xxpay.pay.channel.BaseTrans;
import org.xxpay.pay.channel.yilian.bean.MsgBean;
import org.xxpay.pay.channel.yilian.bean.MsgBody;
import org.xxpay.pay.channel.yilian.encrypt.RSA;
import org.xxpay.pay.channel.yilian.encrypt.TripleDes;
import org.xxpay.pay.channel.yilian.util.Base64;
import org.xxpay.pay.channel.yilian.util.Util;


/**
 * @author: gf
 * @date: 2019-08-14 19:35:44
 * @description: YILIAN代付接口
 */
@Service
public class YilianTransService extends BaseTrans {
    private static final MyLog _log = MyLog.getLog(YilianTransService.class);

    @Override
    public String getChannelName() {
        return PayConstant.CHANNEL_NAME_YILIAN;
    }


    /**
     * 代付
     *
     * @param transOrder
     * @return
     */
    @Override
    public JSONObject trans(TransOrder transOrder) {
        String channelId = transOrder.getChannelId();
        YilianConfig payChannelConfig = new YilianConfig(getTransParam(transOrder));
        JSONObject retObj = new JSONObject();
        try {
            MsgBean req_bean = new MsgBean();
            req_bean.setVERSION("2.1");
            req_bean.setMSG_TYPE("100001");
            req_bean.setBATCH_NO(transOrder.getTransOrderId());//每笔订单不可重复，建议：公司简称缩写+yymmdd+流水号
            req_bean.setUSER_NAME(payChannelConfig.getUserName());//系统后台登录名
            //批量代付可以一次增加多条代付
            MsgBody body = new MsgBody();
            String sn = String.format("%014d", Integer.valueOf("1"));
            body.setSN(sn);//流水号，同一批次不重复即可有多条的情况下自增
            body.setACC_NO(transOrder.getAccountNo());
            body.setACC_NAME(transOrder.getAccountName());
            body.setAMOUNT((transOrder.getAmount() / 100) + "");
            body.setBANK_NAME(transOrder.getBankName());
            body.setACC_PROP(transOrder.getAccountAttr() + "");
            body.setMER_ORDER_NO(transOrder.getMchTransNo());

            req_bean.getBODYS().add(body);


            HttpResponse res = HttpRequest.post(payChannelConfig.getReqUrl())
                    .body(signANDencrypt(req_bean,payChannelConfig))
                    .execute();
            

        } catch (Exception e) {
            _log.error(e.getMessage());
            retObj.put("errDes", "支付操作失败!");
            retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            return retObj;
        }

        return retObj;
    }

    /**
     * 查询订单
     *
     * @param transOrder
     * @return
     */
    @Override
    public JSONObject query(TransOrder transOrder) {
        //String channelId = payOrder.getChannelId();
        YilianConfig payChannelConfig = new YilianConfig(getTransParam(transOrder));
        JSONObject retObj = new JSONObject();

        return retObj;
    }

    /**
     * 余额查询
     *
     * @param payParam
     * @return
     */
    @Override
    public JSONObject balance(String payParam) {
        return null;
    }


    private String signANDencrypt(MsgBean req_bean, YilianConfig payChannelConfig) {
        //商户签名
        req_bean.setMSG_SIGN(RSA.sign(req_bean.toSign(), payChannelConfig.getPfxUrl(), payChannelConfig.getPfxPass()));
        _log.info("req:" + req_bean.toXml());

        //加密报文
        String key = Util.generateKey(9999, 24);
        _log.info("key:" + key);
        String req_body_enc = TripleDes.encrypt(key, req_bean.toXml());
        _log.info("req_body_enc:" + req_body_enc);
        //加密密钥
        String req_key_enc = RSA.encrypt(key, payChannelConfig.getKey());
        _log.info("req_key_enc:" + req_key_enc);
        _log.info("signANDencrypt:" + req_body_enc + "|" + req_key_enc);
        return req_body_enc + "|" + req_key_enc;
    }

}
