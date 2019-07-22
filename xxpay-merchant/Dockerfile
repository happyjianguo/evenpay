FROM openjdk:8-alpine

RUN echo "https://mirror.tuna.tsinghua.edu.cn/alpine/v3.4/main/" > /etc/apk/repositories
RUN apk update \
        && apk upgrade \
        && apk add --no-cache bash \
        bash-doc \
        bash-completion \
        && rm -rf /var/cache/apk/* \
        && /bin/bash
# xxpay-pay/target/xxpay-pay-1.0.0-bin.tar.gz
ADD ./target/xxpay-merchant-1.0.0-bin.tar.gz /home/xxpay/service/
COPY entry.sh /home/xxpay/service/
WORKDIR /home/xxpay/service
CMD ["sh", "entry.sh"]
EXPOSE 8191
