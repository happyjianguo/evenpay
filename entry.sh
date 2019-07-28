#!/bin/sh

APP_HOME=/home/xxpay/service/xxpay-service-1.0.0


APP_MAINCLASS=org.xxpay.service.XxPayServiceApplication


CLASSPATH=$APP_HOME/classes
for i in "$APP_HOME"/lib/*.jar; do
   CLASSPATH="$CLASSPATH":"$i"
done

JAVA_OPTS="-ms512m -mx512m -Xmn128m -Djava.awt.headless=true -XX:MaxPermSize=64m"
java $JAVA_OPTS -classpath $CLASSPATH $APP_MAINCLASS 