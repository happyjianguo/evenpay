package org.xxpay.pay.channel;

import com.alibaba.fastjson.JSONObject;
import org.xxpay.core.entity.PayOrder;

/**
 * 资金归集
 */
public interface CashCollInterface {

    JSONObject coll(PayOrder payOrder);

}
