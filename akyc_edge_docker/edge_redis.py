# -*- coding: utf-8 -*-
import paho.mqtt.client as mqtt
import redis
import threading
import sys
import yaml
import time
import logging

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
# 创建 handler 输出到文件
handler = logging.FileHandler("log/akyc_edge.log", mode='w')
handler.setLevel(logging.INFO)
# handler 输出到控制台
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)
# 创建 logging format
formatter = logging.Formatter("%(asctime)s - %(name)s - %(levelname)s - %(message)s")
handler.setFormatter(formatter)
# add the handlers to the logger
logger.addHandler(handler)
logger.addHandler(ch)
f = open('config/config.yaml')
content = yaml.load(f, Loader=yaml.FullLoader)
debug = content['debug']
saferconHost = content['north']['saferconHost']
raspTopic=content['south']['raspTopic']
saferconTopic=content['north']['saferconTopic']
raspExpire=content['south']['raspExpire']
checkInterval=content['south']['checkInterval']
pool = redis.ConnectionPool(host='redis', port=6379, decode_responses=False)   # host是redis主机，需要redis服务端和客户端都起着 redis默认端口是6379
r = redis.Redis(connection_pool=pool)
DEVICE_LIST=[]
CLOUD_MQTT_CON=False

class AkycCache:
  def __init__(self,r):
    self._r = r
    self.lock = threading.RLock()
  def llen(self):
    return self._r.llen("brokerCache")
  def rpush(self,payload):
    # 加锁
    self.lock.acquire()
    try:
      self._r.rpush("brokerCache", payload)
      if raspExpire:
        self._r.expire("brokerCache", time=raspExpire)
      uid=int.from_bytes(payload[:4],'little')
      print(" +",time.strftime('%Y.%m.%d/%H:%M:%S',time.localtime(time.time())),\
		"(uid)",uid,\
		"(size/num)",payload[6],\
		"/",payload[7],\
		"(cache length)", self.llen())
      self._r.set(uid, 1, checkInterval)
      if uid not in DEVICE_LIST:
       DEVICE_LIST.append(uid)
       logger.info(str(uid)+" connected")
    except Exception as e:
      logger.error(e)
    finally:
      # 修改完成，释放锁
      self.lock.release()
  def lpush(self,payload):
    # 加锁
    self.lock.acquire()
    try:
      self._r.lpush("brokerCache", payload)
      if raspExpire:
        self._r.expire("brokerCache", time=raspExpire)
      print("+ ",time.strftime('%Y.%m.%d/%H:%M:%S',time.localtime(time.time())),\
		"(uid)",int.from_bytes(payload[:4],'little'),\
		"(size/num)",payload[6],\
		"/",payload[7],\
		"(cache length)", self.llen())
    except Exception as e:
      logger.error(e)
    finally:
    # 修改完成，释放锁
      self.lock.release()
  def lpop(self):
    # 加锁
    self.lock.acquire()
    try:
      return self._r.lpop("brokerCache")
    except Exception as e:
      print(e)
    finally:
      # 修改完成，释放锁
      self.lock.release()

akycCache=AkycCache(r) 
# 连接MQTT服务器
def on_mqtt_connect(client,address,port):
  client.connect(address, port, 60)
  client.loop_start()

def on_connect_come(client, userdata, flags, rc):
  _str=str(userdata)
  logger.info(_str+" connected "+str(rc))
  if _str == "cloud":
    global CLOUD_MQTT_CON
    CLOUD_MQTT_CON=True
# 消息处理函数
def on_message_come(client, userdata, msg):
  if(msg.topic and msg.topic == raspTopic):
    akycCache.rpush(msg.payload)
def on_disconnect_come(client, userdata, rc):
  _str=str(userdata)
  logger.info(_str+" disconnected: "+str(rc))
  if _str == "cloud":
    global CLOUD_MQTT_CON
    CLOUD_MQTT_CON=False
def on_log_come(client, userdata, level, buf):
  logger.debug(str(userdata)+str(buf))

def run_southBound(n,cache):
  print("current task：", n, " local mqtt")
  mqttClient = mqtt.Client()
  mqttClient.reinitialise("raspberrypi",True,"local")
  mqttClient.on_message = on_message_come # 消息到来处理函数
  mqttClient.on_disconnect = on_disconnect_come
  mqttClient.on_connect = on_connect_come
  while(True):
    try:
      on_mqtt_connect(mqttClient,"mqtt",1883)
      mqttClient.subscribe(raspTopic, 0)
      break
    except Exception as e:
      print(e)
      logger.error(e)
      time.sleep(10)
      continue
  if debug:
    mqttClient.on_log=on_log_come
  while(True):
    pass

def run_northBound(n,cache):
  print("current task：", n, " cloud mqtt")
  mqttClient2 = mqtt.Client()
  mqttClient2.reinitialise("raspberrypi",True,"cloud")
  mqttClient2.on_disconnect = on_disconnect_come
  mqttClient2.on_connect = on_connect_come
  if debug:
    mqttClient2.on_log=on_log_come
  while(True):
    try:
      on_mqtt_connect(mqttClient2,saferconHost,1883)
      break
    except Exception as e:
      print(e)
      logger.error(e)
      time.sleep(10)
      continue

  while(True):
    while(cache.llen()>0):
      try:
        if CLOUD_MQTT_CON:
          data=akycCache.lpop()
          MQTTMessageInfo=mqttClient2.publish(saferconTopic, data, 1)
          # print(MQTTMessageInfo.is_published())
          print("- ",time.strftime('%Y.%m.%d/%H:%M:%S',time.localtime(time.time())),\
		"(uid)",int.from_bytes(data[:4],'little'),\
		"(size/num)",data[6],\
		"/",data[7],\
		"(cache length)", akycCache.llen())
        else:
           time.sleep(10)
           print("==",time.strftime('%Y.%m.%d/%H:%M:%S',time.localtime(time.time())),
                "transpot suspened due to disconnect with cloud mqtt ",
                "(cache length)", akycCache.llen())
      except Exception as e:
        if data:
          akycCache.lpush(data)
          print("+ ",time.strftime('%Y.%m.%d/%H:%M:%S',time.localtime(time.time())),\
		"(uid)",int.from_bytes(data[:4],'little'),\
		"(size/num)",data[6],\
		"/",data[7],\
		"(cache length)", akycCache.llen())
        logger.error(e)
    pass

def run_checkInterval():
  print("current task：check device list start per",checkInterval) 
  for element in DEVICE_LIST:
    if not r.exists(element):
      logger.warning(str(element)+" disconnected")
      DEVICE_LIST.pop(DEVICE_LIST.index(element))
  global timer
  timer = threading.Timer(checkInterval, run_checkInterval)
  timer.start()

if __name__ == '__main__':
  t1 = threading.Thread(target=run_southBound, args=("thread 1",akycCache))
  t2 = threading.Thread(target=run_northBound, args=("thread 2",akycCache))
  t3 = threading.Timer(checkInterval,run_checkInterval)

  t1.start()
  t2.start()
  t3.start()
  
