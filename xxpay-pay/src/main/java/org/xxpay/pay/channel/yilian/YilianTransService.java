package org.xxpay.pay.channel.yilian;

import cn.hutool.core.util.StrUtil;
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
import org.xxpay.pay.channel.yilian.util.Strings;
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
            //发送网络请求
            HttpResponse res = HttpRequest.post(payChannelConfig.getReqUrl())
                    .body(signANDencrypt(req_bean,payChannelConfig))
                    .execute();
            if (res.isOk()) {
                String temp = res.body();
                _log.info("Yilian 代付请求结果:{}", temp);
                MsgBean res_bean = decryptANDverify(temp,payChannelConfig);
                if (StrUtil.equals("0000",res_bean.getTRANS_STATE(),true)) {
                    //交易成功
                    _log.info("{} >>> 转账成功", "Yilian");
                    retObj.put("status", 2);    // 成功
                } else if (StrUtil.equals("00A4",res_bean.getTRANS_STATE(),true)) {
                    _log.info("{} >>> 转账处理中", "Yilian");
                    retObj.put("status", 1);    // 处理中
                } else {
                    // 交易失败
                    _log.info("{} >>> 转账失败", "Yilian");
                    retObj.put("status", 3);    // 失败
                    retObj.put("channelErrCode",res_bean.getTRANS_STATE());
                }
            }else{
                _log.info("网络访问失败");
                retObj.put("errDes", "代付操作失败!");
                retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
            }
        } catch (Exception e) {
            _log.error(e.getMessage());
            retObj.put("errDes", "代付操作失败!");
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
        String logPrefix = "【易联代付查询】";
        String transCode = "100002";	//订单查询
        JSONObject retObj = buildRetObj();
        try{
            MsgBean req_bean = new MsgBean();
            req_bean.setVERSION("2.1");
            req_bean.setMSG_TYPE(transCode);
            req_bean.setBATCH_NO(transOrder.getTransOrderId());//同代付交易请求批次号
            req_bean.setUSER_NAME(payChannelConfig.getUserName());//系统后台登录名

            //发送网络请求
            HttpResponse res = HttpRequest.post(payChannelConfig.getReqUrl())
                    .body(signANDencrypt(req_bean,payChannelConfig))
                    .execute();
            if (res.isOk()) {
                String temp = res.body();
                _log.info("{} 代付查询请求结果:{}",logPrefix, temp);
                MsgBean res_bean = decryptANDverify(temp,payChannelConfig);
                if (StrUtil.equals("0000",res_bean.getTRANS_STATE(),true)) {
                    //交易成功
                    _log.info("{} >>> 转账成功", "Yilian");
                    retObj.put("status", 2);    // 成功
                } else if (StrUtil.equals("00A4",res_bean.getTRANS_STATE(),true)) {
                    _log.info("{} >>> 转账处理中", "Yilian");
                    retObj.put("status", 1);    // 处理中
                } else {
                    // 交易失败
                    _log.info("{} >>> 转账失败", "Yilian");
                    retObj.put("status", 3);    // 失败
                    retObj.put("channelErrCode",res_bean.getTRANS_STATE());
                }
            }
        }catch (Exception e) {
            _log.error(e, "易联代付查询异常");
            retObj = buildFailRetObj();
            return retObj;
        }
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

    private static MsgBean decryptANDverify(String res,YilianConfig payChannelConfig) {

        String msg_sign_enc = res.split("\\|")[0];
        String key_3des_enc = res.split("\\|")[1];

        //解密密钥
        String key_3des = RSA.decrypt(key_3des_enc, payChannelConfig.getPfxUrl(), payChannelConfig.getPfxPass());

        //解密报文
        String msg_sign = TripleDes.decrypt(key_3des, msg_sign_enc);
        MsgBean res_bean = new MsgBean();
        res_bean.toBean(msg_sign);
        _log.info("res:" + res_bean.toXml());

        //验签
        String dna_sign_msg = res_bean.getMSG_SIGN();
        res_bean.setMSG_SIGN("");
        String verify = Strings.isNullOrEmpty(res_bean.getVERSION()) ? res_bean.toXml() : res_bean.toSign();
        _log.info("verify:" + verify);
        if (!RSA.verify(dna_sign_msg, payChannelConfig.getKey(), verify)) {
            _log.error("验签失败");
            res_bean.setTRANS_STATE("00A0");
        }
        return res_bean;
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
