#!/usr/bin/python
# encoding=utf-8

import sys
import os
from jinja2 import Environment, FileSystemLoader

active=sys.argv[1]
region=sys.argv[2]

print "active = {0}, region = {1}".format(active, region)

env = Environment(loader=FileSystemLoader('./templates', encoding='utf-8'))
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

    template = env.get_template('Dockerfile.j2')
    docker_output = template.render(encoding='utf-8', project=project, version=version, app=app, port=app_opt[app]['port'])
    dockerfile = os.path.join(app_path, 'Dockerfile')
    print "dockerfile = ", dockerfile
    print " output =", docker_output
    with open(dockerfile, 'w') as f:
        f.write(docker_output)
    
    template = env.get_template('entry.sh.j2')
    entry_output = template.render(encoding='utf-8', project=project, version=version, app=app, port=app_opt[app])
    entry_file = os.path.join(app_path, 'entry.sh')
    print "entry_file = ", entry_file
    print " output =", entry_output
    with open(entry_file, 'w') as f:
        f.write(entry_output)

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
    image_name = "registry.cn-{0}.aliyuncs.com/evencc/evenpay-{1}:{2}".format(region, app, active)
    cmd1 = "docker build -t {0} .".format(image_name)
    print cmd1
    os.system(cmd1)

    cmd2 = "docker push {0}".format(image_name)
    print cmd2
    # docker push haodiaodemingzi/evenpay-agent:tagname print cmd2
    os.system(cmd2)

    os.chdir(cwd)
    



