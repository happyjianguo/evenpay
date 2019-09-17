#!/usr/bin/python
# encoding=utf-8

import sys
import os
import re

active = 'dev'
latest = 'latest'
region = None
app = None
repo = None


try:
    active=sys.argv[1]
except:
    # github只能编译 hk 的镜像
    active = 'dev'
try:
    app = sys.argv[2]
except:
    pass


env = os.environ

branch = 'dev'
commit = os.getenv('TRAVIS_COMMIT', 'abcd123')
print "branch === ", branch
print "commit === ", commit
tag = 'latest'

if re.search(r'master', branch):
    tag = 'prod'
    region = 'hongkong'
elif re.search(r'test|staging', branch):
    tag = branch
    region = 'hongkong'
elif re.search(r'dev', branch):
    tag = 'dev'
elif re.search(r'release', branch):
    tag = branch
else:
    tag = branch

print "active = {0}, region = {1}, tag={2}, commit={3}".format(active, region, tag, commit)

project = 'xxpay'
version = '1.0.0'
app_list = ['service', 'agent', 'manage', 'merchant', 'pay', 'task']
if app is not None:
    app_list = app.split(",")

app_opt = {
    'service': { 'port': 8190, 'main': 'org.xxpay.service.XxPayServiceAppliaction' },
    'merchant': { 'port': 8191 },
    'agent': { 'port': 8192 },
    'manage': { 'port': 8193 },
    'task': { 'port': 8194 },
    'pay': { 'port': 3020}
}



# mvn clean build
# os.system("mvn clean install -P" + active)
# for app in app_opt:
for app in app_list:
    app_path = '-'.join([project,app])
    # build docker image and push
    cwd = os.getcwd()
    build_path = os.path.join(cwd, app_path)
    os.chdir(build_path)
    print "build_path = ", build_path

    image_name = "evenpay-{0}:{1}".format(app, active)
    cmd1 = "docker build -t {0} .".format(image_name)
    print cmd1
    rc = os.system(cmd1)
    if rc != 0:
        raise Exception("build image failed")

    os.chdir(cwd)
