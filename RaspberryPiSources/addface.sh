#!/bin/bash
export DISPLAY=:0
if [ ! -z $(pgrep -f "python 03_face_recognizer.py") ]; then
	echo "Killing camera feed"
	kill $(pgrep -f "python 03_face_recognizer.py")
fi

if [ ! -z $(pgrep -f "python 01_face_gatherer.py") ]; then
	echo "Killing dangling face gatherer"
	kill $(pgrep -f "python 01_face_gatherer.py")
fi

rm labels trainer.yml

if [ ! -e "logger.txt" ]; then
	touch logger.txt
fi

printf "Started gathering\n"
python 01_face_gatherer.py "$1"
printf "Gathered\n"
printf "Started training\n"
python 02_face_trainer.py
printf "Trained\n"
nohup python 03_face_recognizer.py &
printf "\n"
