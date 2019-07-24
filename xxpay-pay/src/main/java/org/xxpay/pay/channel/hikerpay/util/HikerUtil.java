package org.xxpay.pay.channel.hikerpay.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;

public class HikerUtil {
    public static String queryParams(long time,String PARTNER_CODE,String CREDENTIAL_CODE) {
        //long time = System.currentTimeMillis();
        String nonceStr = RandomStringUtils.random(15, true, true);
        String validStr = PARTNER_CODE + "&" + time + "&" + nonceStr + "&" + CREDENTIAL_CODE;
        String sign = DigestUtils.sha256Hex(validStr).toLowerCase();
        return "time=" + time + "&nonce_str=" + nonceStr + "&sign=" + sign;
    }

    public static String queryParams2(long time,String PARTNER_CODE,String CREDENTIAL_CODE,String redirect) {
        //long time = System.currentTimeMillis();
        String nonceStr = RandomStringUtils.random(15, true, true);
        String validStr = redirect+ "&" +PARTNER_CODE + "&" + time + "&" + nonceStr + "&" + CREDENTIAL_CODE;
        String sign = DigestUtils.sha256Hex(validStr).toLowerCase();

        return "redirect=" +redirect + "&time=" + time + "&nonce_str=" + nonceStr + "&sign=" + sign;

    }



}
