package org.xxpay.agent.order.ctrl;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.xxpay.agent.common.ctrl.BaseController;
import org.xxpay.agent.common.service.RpcCommonService;
import org.xxpay.core.common.constant.Constant;
import org.xxpay.core.common.domain.XxPayPageRes;
import org.xxpay.core.common.domain.XxPayResponse;
import org.xxpay.core.common.util.DateUtil;
import org.xxpay.core.entity.PayOrder;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping(Constant.AGENT_CONTROLLER_ROOT_PATH + "/pay_order")
public class PayOrderController extends BaseController {

    @Autowired
    private RpcCommonService rpcCommonService;

    /**
     * 查询单条支付记录
     * @return
     */
    @RequestMapping("/get")
    @ResponseBody
    public ResponseEntity<?> get(HttpServletRequest request) {
        JSONObject param = getJsonParam(request);
        String payOrderId = getStringRequired(param, "payOrderId");
        Long agentId = getUser().getId();
        PayOrder queyrPayOrder = new PayOrder();
        queyrPayOrder.setAgentId(agentId);
        queyrPayOrder.setPayOrderId(payOrderId);
        PayOrder payOrder = rpcCommonService.rpcPayOrderService.find(queyrPayOrder);
        return ResponseEntity.ok(XxPayResponse.buildSuccess(payOrder));
    }

    /**
     * 支付订单记录列表
     * @return
     */
    @RequestMapping("/list")
    @ResponseBody
    public ResponseEntity<?> list(HttpServletRequest request) {
        JSONObject param = getJsonParam(request);
        Integer page = getInteger(param, "page");
        Integer limit = getInteger(param, "limit");
        PayOrder payOrder = getObject(param, PayOrder.class);
        payOrder.setAgentId(getUser().getId());
        // 订单起止时间
        Date createTimeStart = null;
        Date createTimeEnd = null;
        String createTimeStartStr = getString(param, "createTimeStart");
        if (StringUtils.isNotBlank(createTimeStartStr)) createTimeStart = DateUtil.str2date(createTimeStartStr);
        String createTimeEndStr = getString(param, "createTimeEnd");
        if (StringUtils.isNotBlank(createTimeEndStr)) createTimeEnd = DateUtil.str2date(createTimeEndStr);

        int count = rpcCommonService.rpcPayOrderService.count(payOrder, createTimeStart, createTimeEnd);
        if (count == 0) return ResponseEntity.ok(XxPayPageRes.buildSuccess());
        List<PayOrder> payOrderList = rpcCommonService.rpcPayOrderService.select(
                (getPageIndex(page) - 1) * getPageSize(limit), getPageSize(limit), payOrder, createTimeStart, createTimeEnd);

        // 代理商户和商户不能显示扣量订单，需要把扣量的单子加入到失败的订单记录里，
        // 当status=-1的时候,设置status=5再查询一次，push到返回list
        if (payOrder.getStatus() == -1) {
            payOrder.setStatus((byte) 5);
        }
        List<PayOrder> dedutionList = rpcCommonService.rpcPayOrderService.select(
                (getPageIndex(page) - 1) * getPageSize(limit), getPageSize(limit), payOrder, createTimeStart, createTimeEnd);
        if (dedutionList.isEmpty()){
            return ResponseEntity.ok(XxPayPageRes.buildSuccess(payOrderList, count));
        }
        List<PayOrder> joinedList = Stream.of(payOrderList, dedutionList)
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());

        // 代理商户和商户不能显示扣量订单，需要把扣量的单子加入到失败的订单记录里，
        // 当status=-1的时候,设置status=5再查询一次，push到返回list

        return ResponseEntity.ok(XxPayPageRes.buildSuccess(joinedList, count));
    }

    /**
     * 查询订单统计数据
     * @return
     */
    @RequestMapping("/count")
    @ResponseBody
    public ResponseEntity<?> count(HttpServletRequest request) {
        JSONObject param = getJsonParam(request);
        String payOrderId = getString(param, "payOrderId");
        String mchOrderNo = getString(param, "mchOrderNo");
        Long productId = getLong(param, "productId");
        Long mchId = getLong(param, "mchId");
        Byte productType = getByte(param, "productType");
        Long angentId = getUser().getId();

        // 订单起止时间
        String createTimeStartStr = getString(param, "createTimeStart");
        String createTimeEndStr = getString(param, "createTimeEnd");
        Map allMap = rpcCommonService.rpcPayOrderService.count4All(angentId, mchId, productId, payOrderId, mchOrderNo, productType, createTimeStartStr, createTimeEndStr);
        Map successMap = rpcCommonService.rpcPayOrderService.count4Success(angentId, mchId, productId, payOrderId, mchOrderNo, productType, createTimeStartStr, createTimeEndStr);
        Map failMap = rpcCommonService.rpcPayOrderService.count4Fail(angentId, mchId, productId, payOrderId, mchOrderNo, productType, createTimeStartStr, createTimeEndStr);

        JSONObject obj = new JSONObject();
        obj.put("allTotalCount", allMap.get("totalCount"));                         // 所有订单数
        obj.put("allTotalAmount", allMap.get("totalAmount"));                       // 总金额
        obj.put("successTotalCount", successMap.get("totalCount"));                 // 成功订单数
        obj.put("successTotalAmount", successMap.get("totalAmount"));               // 成功金额
        obj.put("successTotalMchIncome", successMap.get("totalMchIncome"));         // 成功商户收入
        obj.put("successTotalAgentProfit", successMap.get("totalAgentProfit"));     // 成功代理商利润
        obj.put("failTotalCount", failMap.get("totalCount"));                       // 未完成订单数
        obj.put("failTotalAmount", failMap.get("totalAmount"));                     // 未完成金额
        return ResponseEntity.ok(XxPayResponse.buildSuccess(obj));
    }

}