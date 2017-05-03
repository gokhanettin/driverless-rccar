#!/usr/bin/env python3
# -*- coding: utf-8 -*-


import socket
import sys
import os
import datetime
import cv2
import numpy as np


HOST = "0.0.0.0"
PORT = 5000

DATASETDIR = "/tmp/dataset"

if len(sys.argv) > 1:
    DATASETDIR = sys.argv[1]

IMAGEDIR = DATASETDIR + "/im"

DATASETFILE = "dataset.txt"
IMAGEFILE = "im{:08d}.jpeg"

DATASETFILEFULL = DATASETDIR + "/" + DATASETFILE
IMAGEFILEFULL = IMAGEDIR + "/" + IMAGEFILE
DATASETFILEROW = "{timestep};{timestamp};{imagefile};{steering_cmd};{speed_cmd};{steering:.3f};{speed:.3f}\n"


def main():

    # Won't overwrite an existing dataset
    os.makedirs(DATASETDIR)
    os.makedirs(IMAGEDIR)

    mySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    mySocket.bind((HOST, PORT))
    mySocket.listen(1)
    image_id = 0
    while True:
        print("Waiting for a new connection")
        conn, addr = mySocket.accept()
        datasetfile = open(DATASETFILEFULL, 'a')
        timestep = 0

        has_connection = True
        try:
            print("Connection from: " + str(addr))
            while True:
                timestep += 1
                image_id += 1
                buff = b''
                while True:
                    c = conn.recv(1)
                    if c == b'$':
                        print("Client asked to disconnect")
                        conn.close()
                        datasetfile.close()
                        has_connection = False
                        break
                    elif c == b'[':
                        continue
                    elif c == b']':
                        break
                    else:
                        buff += c

                if not has_connection:
                    break

                decoded_buff = buff.decode()
                header = decoded_buff.split(';')
                speed_cmd = int(header[0])
                steering_cmd = int(header[1])
                speed = float(header[2])
                steering = float(header[3])
                size = int(header[4])

                print(timestep, speed_cmd, steering_cmd, speed, steering, size)
                img = bytearray()
                while size > 0:
                    chunk = conn.recv(size)
                    img.extend(chunk)
                    size -= len(chunk)

                with open(IMAGEFILEFULL.format(image_id), 'wb') as f:
                    f.write(img)

                np_arr = np.frombuffer(img, dtype=np.uint8)
                frame = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)
                cv2.imshow('acquisition', frame)

                if (cv2.waitKey(1) & 0xFF) == ord('q'):  # Hit `q` to exit
                    break

                # timestamp in miliseconds
                timestamp = round(datetime.datetime.now().timestamp() * 1000)

                datasetfile.write(DATASETFILEROW.format(
                    timestep=timestep, timestamp=timestamp,
                    speed_cmd=speed_cmd, steering_cmd=steering_cmd,
                    speed=speed, steering=steering,
                    imagefile=IMAGEFILE.format(image_id)))
                if image_id % 20:
                    datasetfile.flush()

        except KeyboardInterrupt:
            print("Server asked to disconnect")
            conn.close()
            datasetfile.close()

if __name__ == '__main__':
    main()
