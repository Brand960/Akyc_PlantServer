# -*- coding: utf-8 -*-
import paho.mqtt.client as mqtt
import redis
import threading
import sys
import yaml

f = open('config.yaml')
content = yaml.load(f, Loader=yaml.FullLoader)
saferconHost = content['north']['saferconHost']
raspTopic=content['south']['raspTopic']
saferconTopic=content['north']['saferconTopic']
mqttClient = mqtt.Client()
mqttClient2 = mqtt.Client()
pool = redis.ConnectionPool(host='redis', port=6379, decode_responses=False)   # host是redis主机，需要redis服务端和客户端都起着 redis默认端口是6379
r = redis.Redis(connection_pool=pool)

# 连接MQTT服务器
def on_mqtt_connect(client,address,port):
  client.connect(address, port, 60)
  client.loop_start()
  print(address+" mqtt server connected")
 
# 消息处理函数
def on_message_come(client, userdata, msg):
  if(msg.topic and msg.topic == raspTopic):
    try:
      r.rpush("brokerCache", msg.payload)
      r.expire("brokerCache", time=120)
      print("+ current redis broker length: ", r.llen("brokerCache"))
    except BaseException:
        pass

def run_southernBound(n):
  print("current task：", n)
  mqttClient.on_connect = on_mqtt_connect(mqttClient,"mqtt",1883)
  mqttClient.subscribe(raspTopic, 0)
  mqttClient.on_message = on_message_come # 消息到来处理函数

  while(True):
    pass

def run_northernBound(n):
  print("current task：", n)
  mqttClient2.on_connect = on_mqtt_connect(mqttClient2,saferconHost,1883)

  while(True):
    while(r.llen("brokerCache")>0):
      try:
        mqttClient2.publish(saferconTopic, r.lpop("brokerCache"), 0)
        print("- current redis broker length: ", r.llen("brokerCache"))
      except BaseException:
        continue
    pass

if __name__ == '__main__':
  r.flushdb()
  t1 = threading.Thread(target=run_southernBound, args=("thread 1",))
  t1.start()

  t2 = threading.Thread(target=run_northernBound, args=("thread 2",))
  t2.start()
  
