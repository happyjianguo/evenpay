#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os,sys,re,traceback
from datetime import datetime
from string import Template
import json
def generateAndroidJavaCode(package_name, class_name, file_name,template):
    channelName = class_name.upper()
    path = "./" + class_name
    filePath = r'%s/%s' % (path,file_name)
    print("filePath: %s",filePath)
    if os.path.exists(path) == False:
        os.makedirs(path)
    class_file = open(filePath,'w')
    lines = []
    #模版文件
    template_file = open(template,'r')
    tmpl = Template(template_file.read())
    #模版替换
    lines.append(tmpl.substitute(
                PACKAGE_NAME = package_name,
                DATA = datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                CHANNEL_NAME = channelName,
                CLASS_NAME = class_name))
    # 0.将生成的代码写入文件
    class_file.writelines(lines)
    class_file.close()
    print ('generate %s over. ~ ~' % filePath)
## 1. 解析配置文件，根据渠道名称修改channelList值
configuration = """
{
  "packgename":"org.xxpay.pay.channel",
  "channelList":[
    "Hiker",
    "Synopay"
  ],
  "templates":[
      "BaseConfig.java.tpl",
      "BasePaymentService.java.tpl",
      "BasePayNotifyService.java.tpl",
      "BaseRefundService.java.tpl"
  ]
}
"""
configurationJson = json.loads(configuration)
## 2. 按照模板生成对应的文件
for channel in configurationJson["channelList"]:
    packgename = '%s.%s' % (configurationJson["packgename"],channel.lower())
    for template in configurationJson["templates"]:
        filename = template.replace("Base",channel).replace(".tpl","")
        print ("filename %s" % filename)
        generateAndroidJavaCode(packgename,channel,filename,template)