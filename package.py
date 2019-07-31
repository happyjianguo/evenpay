#!/usr/bin/python
# encoding=utf-8

import sys
import os
import re

active = sys.argv[1]
latest = 'latest'
region = None
app = None


try:
    region=sys.argv[2]
except:
    region = "shenzhen"
try:
    app = sys.argv[3]
except:
    pass


env = os.environ

branch = os.getenv('TRAVIS_REPO_SLUG', active)
commit = os.getenv('TRAVIS_COMMIT', 'abcd123')
tag = 'latest'

if re.search(r'master|release', branch):
    tag = 'prod'
    region = 'hongkong'
    active = 'prod'
elif re.search(r'test|staging', branch):
    tag = branch
    active = 'test'
elif re.search(r'dev', branch):
    tag = 'dev'
    active = 'dev'
else:
    tag = active

print(os.environ['HOME'])
print "active = {0}, region = {1}, tag={2}, commit={3}".format(active, region, tag, commit))

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
os.system("mvn clean install -P" + tag)
# for app in app_opt:
for app in app_list:
    app_path = '-'.join([project,app])
    # build docker image and push
    cwd = os.getcwd()
    build_path = os.path.join(cwd, app_path)
    os.chdir(build_path)
    print "build_path = ", build_path
    for item in (tag, commit):
        image_name = "registry.cn-{0}.aliyuncs.com/evencc/evenpay-{1}:{2}".format(region, app, item)
        cmd1 = "docker build -t {0} .".format(image_name)
        print cmd1
        os.system(cmd1)

        cmd2 = "docker push {0}".format(image_name)
        print cmd2
        os.system(cmd2)

    os.chdir(cwd)
