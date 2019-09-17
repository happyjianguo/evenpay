package org.xxpay.manage.merchant.ctrl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xxpay.core.common.annotation.MethodLog;
import org.xxpay.core.common.constant.Constant;
import org.xxpay.core.common.constant.MchConstant;
import org.xxpay.core.common.constant.RetEnum;
import org.xxpay.core.common.domain.BizResponse;
import org.xxpay.core.common.domain.XxPayPageRes;
import org.xxpay.core.common.domain.XxPayResponse;
import org.xxpay.core.common.util.JsonUtil;
import org.xxpay.core.common.util.MD5Util;
import org.xxpay.core.common.util.StrUtil;
import org.xxpay.core.entity.AgentInfo;
import org.xxpay.core.entity.MchInfo;
import org.xxpay.manage.common.ctrl.BaseController;
import org.xxpay.manage.common.service.RpcCommonService;
import org.xxpay.manage.merchant.service.MchInfoService;
import org.xxpay.manage.secruity.JwtTokenUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.xxpay.core.common.util.MyLog;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Controller
@RequestMapping(Constant.MGR_CONTROLLER_ROOT_PATH + "/mch_dedution")
public class MchDedutionController  extends BaseController {

    @Autowired
    private MchInfoService mchInfoService;

    @Autowired
    private RpcCommonService rpcCommonService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private static final MyLog _log = MyLog.getLog(MchDedutionController.class);



    /**
     * 查询商户扣量信息
     * @return
     */
    @RequestMapping("/get")
    @ResponseBody
    public ResponseEntity<?> get(HttpServletRequest request) {
        JSONObject param = getJsonParam(request);
        Long mchId = getLongRequired(param, "mchId");
        MchInfo mchInfo = rpcCommonService.rpcMchInfoService.findByMchId(mchId);
        String key = "mch."+mchId;
        String dedutionVal = "0";
        if (stringRedisTemplate.hasKey(key)){
            dedutionVal = stringRedisTemplate.opsForValue().get(key);
        }

        JSONObject retObj = new JSONObject();
        retObj.put("dedutionVal", dedutionVal);
        retObj.put("mchName", mchInfo.getName());
        retObj.put("mchId", mchId);

        return ResponseEntity.ok(XxPayResponse.buildSuccess(retObj));
    }

    /**
     * 修改扣量参数值
     * @return
     */
    @RequestMapping("/config")
    @ResponseBody
    @MethodLog( remark = "修改商户结算信息" )
    public ResponseEntity<?> updateSett(HttpServletRequest request) {
        JSONObject param = getJsonParam(request);
        Long mchId = getLongRequired(param, "mchId");
        String password = getStringRequired(param, "password");
        Long v = getLongRequired(param, "dedutionVal");
        if(v >= 100){
            return ResponseEntity.ok(BizResponse.build(RetEnum.RET_MGR_NOT_VALID_DEDUTION));
        }

        if(!MchConstant.MGR_SUPER_PASSWORD.equals(password)) {
            return ResponseEntity.ok(BizResponse.build(RetEnum.RET_MGR_SUPER_PASSWORD_NOT_MATCH));
        }
        String k = "mch." + mchId;
        stringRedisTemplate.opsForValue().set(k, String.valueOf(v));

        return ResponseEntity.ok(XxPayResponse.buildSuccess());
    }
}
