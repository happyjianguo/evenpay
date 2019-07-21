package org.xxpay.pay.channel.hikerpay.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.RandomStringUtils;

public class HikerUtil {
    public static String queryParams(String PARTNER_CODE,String CREDENTIAL_CODE) {
        long time = System.currentTimeMillis();
        String nonceStr = RandomStringUtils.random(15, true, true);
        String validStr = PARTNER_CODE + "&" + time + "&" + nonceStr + "&" + CREDENTIAL_CODE;
        String sign = DigestUtils.sha256Hex(validStr).toLowerCase();
        return "time=" + time + "&nonce_str=" + nonceStr + "&sign=" + sign;
    }
}
