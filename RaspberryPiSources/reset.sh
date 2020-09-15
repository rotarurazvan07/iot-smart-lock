export DISPLAY=:0
if [ ! -z $(pgrep -f "python 03_face_recognizer.py") ]; then
	kill $(pgrep -f "python 03_face_recognizer.py")
fi

rm -rf dataset/
rm labels
rm trainer.yml
nohup python 03_face_recognizer.py &