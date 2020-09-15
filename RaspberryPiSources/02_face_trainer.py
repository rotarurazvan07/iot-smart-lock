import os
import numpy as np 
from PIL import Image 
import cv2
import pickle

import logging

log_format = "Trainer: %(asctime)s\t%(message)s"
logging.basicConfig(filename='logger.txt', level='DEBUG', format=log_format)

faceCascade = cv2.CascadeClassifier("haarcascade_frontalface_alt2.xml")
recognizer = cv2.face.LBPHFaceRecognizer_create()

baseDir = os.path.dirname(os.path.abspath(__file__))
imageDir = os.path.join(baseDir, "dataset")

currentId = 1
labelIds = {}
yLabels = []
xTrain = []

for root, dirs, files in os.walk(imageDir):
    for file in files:
        logging.info(file)
        if file.endswith("png") or file.endswith("jpg"):
            path = os.path.join(root, file)
            label = os.path.basename(root)
            logging.info(label)

            if not label in labelIds:
                labelIds[label] = currentId
                currentId += 1

            id_ = labelIds[label]
            pilImage = Image.open(path).convert("L")
            imageArray = np.array(pilImage, "uint8")
            faces = faceCascade.detectMultiScale(imageArray,
                                         scaleFactor=1.05,
                                         minNeighbors=3,
                                         minSize=(30,30))
    
            for (x, y, w, h) in faces:
                roi = imageArray[y:y+h, x:x+w]
                xTrain.append(roi)
                yLabels.append(id_)

try:
    recognizer.train(xTrain, np.array(yLabels))
    recognizer.save("trainer.yml")
    
    with open("labels", "wb") as f:
        pickle.dump(labelIds, f)
        f.close()
except:
    logging.info("No faces to train")

logging.info(labelIds)
logging.info("Trained")
