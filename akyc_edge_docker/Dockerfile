# edge example
#
# VERSION               0.1
FROM python:3.8
MAINTAINER Yuepeng <brand960@foxmail.com>
RUN mkdir -p /home/app
WORKDIR /home/app

ADD . /home/app
RUN pip3 install -i https://pypi.tuna.tsinghua.edu.cn/simple --upgrade pip && pip3 install -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt

CMD python3 edge_redis.py

