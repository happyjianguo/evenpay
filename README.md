# xxpay4dubbo
xxpay商业版本，使用 springboot + dubbo 架构开发,支持分布式部署.

### 开发说明

xxpay-generator 生成mybatis代码,然后将model拷贝到xxpay-core项目中,将mapper拷贝到xxpay-service项目中,拷贝mapper时要比对是否有修改

xxpay-service 为dubbo服务生产者,所有与数据库操作,或公共的的业务逻辑都封装此业务层

xxpay-core 为公共方法,dubbo服务接口以及实体bean,每个项目都需要引用

xxpay-manage 运营管理平台的接口

xxpay-merchant 商户系统的接口

xxpay-agent 代理商系统的接口

xxpay-pay 支付核心,所有支付渠道对接实现

xxpay-task 定时任务,包括对账服务,结算服务.部署时需单节点部署

#### xxpay4dubbo
| 项目  | 端口 | 描述
|---|---|---
|xxpay-core |  | 公共方法,实体Bean,API接口定义
|xxpay-generator |  | mybatis数据访问层生成代码
|xxpay-manage | 8193 | 运营平台接口
|xxpay-merchant | 8191 | 商户系统接口
|xxpay-agent | 8192 | 代理商系统接口
|xxpay-pay | 3020 | 支付核心系统
|xxpay-service |  | 业务接口
|xxpay-task | 8194 | 定时任务,包括对账和结算服务