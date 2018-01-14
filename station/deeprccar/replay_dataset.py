#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

import cv2

import dataset as ds
from utils import get_mapped_steering_command
from utils import get_mapped_speed_command
from collections import deque

parser = argparse.ArgumentParser()
parser.add_argument("dataset_dir")
args = parser.parse_args()

dataset_file = args.dataset_dir + "/" + ds.DATASET_FILE
image_dir = args.dataset_dir + "/" + ds.IMAGE_FOLDER + "/"
with open(dataset_file, 'r') as csv:
    for line in csv.readlines():
        line = line.strip()
        (_, _, imagefile, steering_cmd, speed_cmd, _, _) = line.split(";")
        steering_cmd = float(steering_cmd)
        speed_cmd = float(speed_cmd)

        image = cv2.imread(image_dir + imagefile, cv2.IMREAD_COLOR)
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        image = cv2.resize(image, (640, 480), interpolation=cv2.INTER_CUBIC)
        # Count images
        cv2.putText(image, imagefile, (5, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, 255)

        cv2.putText(image, "Steering Command: {}".
                    format(steering_cmd), (5, 475),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, 255)

        steering_cmd = get_mapped_steering_command(steering_cmd)
        cv2.line(image, (240, 300), (240 - steering_cmd, 200),
                (0, 255, 0), 3)

        speed_cmd = get_mapped_speed_command(speed_cmd)
        cv2.line(image, (50, 160), (50, 160 - speed_cmd),
                (0, 255, 0), 3)

        cv2.imshow('replay dataset', cv2.cvtColor(image, cv2.COLOR_RGB2BGR))
        if (cv2.waitKey(0) & 0xFF) == ord('q'):  # Hit `q` to exit
            break
