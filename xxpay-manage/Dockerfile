FROM openjdk:8-alpine

RUN apk update \
        && apk upgrade \
        && apk add --no-cache bash curl \
        bash-doc \
        bash-completion \
        tzdata \
        && rm -rf /var/cache/apk/* \
        && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \ 
        && echo "Asia/Shanghai" > /etc/timezone \
        && /bin/bash
        
ADD ./target/xxpay-manage-1.0.0-bin.tar.gz /home/xxpay/service/
COPY entry.sh /home/xxpay/service/
COPY wait.sh /home/xxpay/service/

WORKDIR /home/xxpay/service
RUN  chmod +x wait.sh 

CMD ["sh", "entry.sh"]
EXPOSE 8193
