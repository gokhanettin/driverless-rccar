#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import argparse
import csv

import cv2

import dataset as ds
from utils import STEERING_COMMAND_NEUTRAL

parser = argparse.ArgumentParser()
parser.add_argument("dataset_dir")
args = parser.parse_args()

datasetfile = args.dataset_dir + "/" + ds.DATASET_FILE
imagedir = args.dataset_dir + "/" + ds.IMAGE_FOLDER + "/"
tmpfile = datasetfile  + ".tmp"

print("Flipping dataset images along with steering commands and angles...")

with open(tmpfile, 'w') as tmp:
    writer = csv.writer(tmp, delimiter=';')
    with open(datasetfile, 'r') as dsf:
        for line in dsf.readlines():
            (timestep, timestamp, imagefile, steering_cmd, speed_cmd,
             steering, speed) = line.strip().split(";")

            # Mirror steering commands and angles
            steering_cmd = 2 * STEERING_COMMAND_NEUTRAL - int(steering_cmd)
            steering = -float(steering)

            # Mirror image with respect to y axis
            image = cv2.imread(imagedir + imagefile, cv2.IMREAD_COLOR)
            image = cv2.flip(image, 1)
            imagefile = "mi" + imagefile[2:]
            cv2.imwrite(imagedir + imagefile, image,
                        [cv2.IMWRITE_JPEG_QUALITY, 100])
            writer.writerow((timestep, timestamp, imagefile, steering_cmd,
                             speed_cmd, steering, speed))

with open(datasetfile, 'a') as dsf:
    with open(tmpfile, 'r') as tmp:
        for line in tmp.readlines():
            dsf.write(line)

os.remove(tmpfile)
