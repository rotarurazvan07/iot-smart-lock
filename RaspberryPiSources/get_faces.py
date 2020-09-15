import pickle

try:
    f = open('labels', 'rb')
    dicti = pickle.load(f)
    f.close()
    for k in dicti.keys():
        print(k)
    if len(dicti) == 0:
        print("No faces")
except:
    print("No faces")