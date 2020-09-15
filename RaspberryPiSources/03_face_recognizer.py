import cv2
from picamera.array import PiRGBArray
from picamera import PiCamera
import numpy as np 
import pickle
import time
from time import sleep
import signal
import sys
import logging
import RPi.GPIO as GPIO
GPIO.setmode(GPIO.BOARD)
GPIO.setup(40, GPIO.OUT, initial=GPIO.LOW)

TIMER_UP = 0
USERS_REGISTERED = 0

log_format = "Recognizer: %(asctime)s\t%(message)s"
logging.basicConfig(filename='logger.txt', level='DEBUG', format=log_format)
logging.info("Started recognizer")
def receiveSignal(signalNumber, frame):
    logging.info("Killing camera")
    camera.close()
    GPIO.cleanup()
    sys.exit(1)
    
signal.signal(signal.SIGTERM, receiveSignal)
detections = dict()
detections["unknown"] = 0
try:
    f = open('labels', 'rb')
    dicti = pickle.load(f)
    f.close()
    logging.info(dicti)
    USERS_REGISTERED = len(dicti)
    for k in dicti.keys():
        detections[k] = 0
except:
    USERS_REGISTERED = 0


camera = PiCamera()
camera.resolution = (640, 480)
camera.framerate = 30
camera.rotation = 180
rawCapture = PiRGBArray(camera, size=(640, 480))


faceCascade = cv2.CascadeClassifier("haarcascade_frontalface_alt2.xml")
recognizer = cv2.face.LBPHFaceRecognizer_create()
if USERS_REGISTERED != 0:
    recognizer.read("trainer.yml")

font = cv2.FONT_HERSHEY_SIMPLEX
cv2.namedWindow('preview', cv2.WINDOW_NORMAL)
cv2.resizeWindow('preview', 320, 480)
curr_time = time.time()
prev_time = time.time()

for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):
    frame = frame.array
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = faceCascade.detectMultiScale(gray,
                                         scaleFactor=1.05,
                                         minNeighbors=5,
                                         minSize=(200, 200))
    
    for (x, y, w, h) in faces:
        roiGray = gray[y:y+h, x:x+w]
        cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
        
        name = "unknown"
        if USERS_REGISTERED != 0:
            id_, conf = recognizer.predict(roiGray)
            print(conf)
            if conf <= 80:
                for k,v in dicti.items():
                    if v == id_:
                        name = k

        logging.info("Detected " + name)
        
        curr_time = time.time()
        
        if TIMER_UP == 0:
            prev_time = curr_time
            TIMER_UP = 1
        
        if curr_time > prev_time + 5:
            logging.info("time expired")
            TIMER_UP = 0
            curr_time = time.time()
            prev_time = curr_time
            for k,v in detections.items():
                detections[k] = 0
        
        if TIMER_UP == 1:
            detections[name] += 1
            if detections[name] > 10:
                if name != "unknown":
                    TIMER_UP = 0
                for k,v in detections.items():
                    detections[k] = 0
                if name == "unknown":
                    logging.info(name + " is at the door")
                else:
                    logging.info("Acces granted to " + name)
                    GPIO.output(40, GPIO.HIGH)
                    sleep(30)
                    GPIO.output(40, GPIO.LOW)
                curr_time = time.time()
                prev_time = curr_time
        

        
        
        cv2.putText(frame, name, (x,y), cv2.FONT_HERSHEY_SIMPLEX, 1, (255,255,255), 2)
    
    fbframe = cv2.resize(frame, (320, 480))
    cv2.imshow('preview',fbframe)
    
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break
    
    rawCapture.truncate(0)
