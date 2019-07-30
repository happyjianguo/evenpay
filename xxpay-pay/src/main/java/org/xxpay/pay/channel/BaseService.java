package org.xxpay.pay.channel;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.xxpay.core.common.constant.PayConstant;
import org.xxpay.core.common.util.MyLog;

import java.io.File;
import java.io.IOException;

/**
 * @author: dingzhiwei
 * @date: 17/12/24
 * @description:
 */
public class BaseService {

    private static final MyLog _log = MyLog.getLog(BaseService.class);

    protected JSONObject buildRetObj() {
        JSONObject retObj = new JSONObject();
        retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_SUCCESS);
        return retObj;
    }

    protected JSONObject buildFailRetObj() {
        JSONObject retObj = new JSONObject();
        retObj.put(PayConstant.RETURN_PARAM_RETCODE, PayConstant.RETURN_VALUE_FAIL);
        return retObj;
    }

    protected JSONObject buildRetObj(String retValue, String retMsg) {
        JSONObject retObj = new JSONObject();
        retObj.put(PayConstant.RETURN_PARAM_RETCODE, retValue);
        retObj.put(PayConstant.RETURN_PARAM_RETMSG, retMsg);
        return retObj;
    }

    /**
     * 获取证书文件路径
     * @param channelName
     * @param fileName
     * @return
     */
    public String getCertFilePath(String channelName, String certRootPath, String fileName) {
        return certRootPath + File.separator + channelName + File.separator + fileName;
    }

    //发送请求
    protected JSONObject sendHttp(HttpUriRequest request) {
        JSONObject resObj = null;
        CloseableHttpResponse response;
        CloseableHttpClient client = null;
        try {
            client = HttpClients.createDefault();
            response = client.execute(request);
            resObj = procRes(response);
        }catch (Exception e){
            return resObj;
        }finally {
            if(client != null){
                try {
                    client.close();
                } catch (IOException e) {
                    _log.error(e, "");
                }
            }
        }
        return resObj;
    }

    protected JSONObject procRes(CloseableHttpResponse response) throws IOException {
        JSONObject res = null;
        return res;
    }

}
