#!/usr/bin/python
# encoding=utf-8

import sys
import os
import re

active = sys.argv[0]
latest = 'latest'
region='hongkong'

env = os.environ
branch = os.getenv('BITBUCKET_BRANCH', 'master')
commit = os.getenv('BITBUCKET_COMMIT', 'abcd123')
tag = 'latest'

if re.search(r'master|release', branch):
    tag = 'latest'
if re.search(r'test|staging', branch):
    tag = branch
if re.search(r'dev', branch):
    tag = 'dev'

print(os.environ['HOME'])
print "active = {0}, region = {1}, tag={2}, commit={3}".format(active, region, tag, commit)

project = 'xxpay'
version = '1.0.0'
app_list = ['service', 'agent', 'manage', 'merchant', 'pay', 'task']
app_opt = {
    'service': { 'port': 8190, 'main': 'org.xxpay.service.XxPayServiceAppliaction' },
    'merchant': { 'port': 8191 },
    'agent': { 'port': 8192 },
    'manage': { 'port': 8193 },
    'task': { 'port': 8194 },
    'pay': { 'port': 3020}
}


for app in app_opt:
    app_path = '-'.join([project,app])
    # build docker image and push
    cwd = os.getcwd()
    build_path = os.path.join(cwd, app_path)
    os.chdir(build_path)
    print "build_path = ", build_path
    #  IMAGE_NAME=haodiaodemingzi/cloudfeet:$BITBUCKET_COMMIT
    # docker push haodiaodemingzi/evenpay-agent:tagname

    # $ sudo docker login --username=赚钱的大氓 registry.cn-shenzhen.aliyuncs.com
    # $ sudo docker tag [ImageId] registry.cn-shenzhen.aliyuncs.com/evencc/evenpay:[镜像版本号]
    # $ sudo docker push registry.cn-shenzhen.aliyuncs.com/evencc/evenpay:[镜像版本号]
    #image_name = "registry.cn-shenzhen.aliyuncs.com/evencc/evenpay-{0}:latest".format(app)
    for item in (tag, commit):
        image_name = "registry.cn-{0}.aliyuncs.com/evencc/evenpay-{1}:{2}".format(region, app, item)
        cmd1 = "docker build -t {0} .".format(image_name)
        print cmd1
        os.system(cmd1)

        cmd2 = "docker push {0}".format(image_name)
        print cmd2
        # docker push haodiaodemingzi/evenpay-agent:tagname print cmd2
        os.system(cmd2)

    os.chdir(cwd)
