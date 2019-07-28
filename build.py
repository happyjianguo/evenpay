#!/usr/bin/python
# encoding=utf-8

import sys
import os
from jinja2 import Environment, FileSystemLoader

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
    



