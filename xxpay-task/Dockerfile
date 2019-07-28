FROM openjdk:8-alpine

RUN apk update \
        && apk upgrade \
        && apk add --no-cache bash curl \
        bash-doc \
        bash-completion \
        && rm -rf /var/cache/apk/* \
        && /bin/bash

ADD ./target/xxpay-task-1.0.0-bin.tar.gz /home/xxpay/service/
COPY entry.sh /home/xxpay/service/
COPY wait.sh /home/xxpay/service/

WORKDIR /home/xxpay/service
RUN  chmod +x wait.sh 

CMD ["sh", "entry.sh"]
EXPOSE 8194
