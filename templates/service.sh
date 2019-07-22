#!/bin/sh

#Java程序所在的目录（classes的上一级目录）
APP_HOME=/home/xxpay/service/xxpay-{{ app }}-{{ version }}

#需要启动的Java主程序（main方法类）
APP_MAINCLASS=org.xxpay.XxPay{{ app | capitalize }}Application

#拼凑完整的classpath参数，包括指定lib目录下所有的jar
CLASSPATH=$APP_HOME/classes
for i in "$APP_HOME"/lib/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

#java虚拟机启动参数
JAVA_OPTS="-ms512m -mx512m -Xmn128m -Djava.awt.headless=true -XX:MaxPermSize=64m"
java $JAVA_OPTS -classpath $CLASSPATH $APP_MAINCLASS 
