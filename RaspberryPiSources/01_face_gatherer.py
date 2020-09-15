import cv2
from picamera.array import PiRGBArray
from picamera import PiCamera
import numpy as np 
import os
import sys
import shutil

import logging
log_format = "Gatherer: %(asctime)s\t%(message)s"
logging.basicConfig(filename='logger.txt', level='DEBUG', format=log_format)

camera = PiCamera()
camera.resolution = (640, 480)
camera.rotation = 180
camera.framerate = 30
rawCapture = PiRGBArray(camera, size=(640, 480))

faceCascade = cv2.CascadeClassifier("haarcascade_frontalface_alt2.xml")

dirName = "./dataset/" + sys.argv[1]
logging.info(dirName)
try:
    shutil.rmtree(dirName)
    os.makedirs(dirName)
except:
    os.makedirs(dirName)
logging.info("Directory Created")
cv2.namedWindow('preview', cv2.WINDOW_NORMAL)
cv2.resizeWindow('preview', 320, 480)
count = 1
for frame in camera.capture_continuous(rawCapture, format="bgr", use_video_port=True):
    if count > 100:
        break
    frame = frame.array
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
    faces = faceCascade.detectMultiScale(gray,
                                         scaleFactor=1.05,
                                         minNeighbors=5,
                                         minSize=(200, 200))
    for (x, y, w, h) in faces:
        roiGray = gray[y:y+h, x:x+w]
        fileName = dirName + "/" + sys.argv[1] + str(count) + ".jpg"
        logging.info(fileName)
        cv2.imwrite(fileName, roiGray)
        cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
        cv2.putText(frame, str(count), (x,y), cv2.FONT_HERSHEY_SIMPLEX, 1, (255,255,255), 2)
        count += 1
    fbframe = cv2.resize(frame, (320,480))
    cv2.imshow('preview',fbframe)
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break
    
    rawCapture.truncate(0)
        
camera.close()
logging.info("Gathered")
