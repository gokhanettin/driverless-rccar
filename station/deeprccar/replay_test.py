#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

import numpy as np
import cv2

from utils import get_mapped_steering_command
from utils import get_mapped_speed_command


parser = argparse.ArgumentParser()
parser.add_argument("csv_file")
parser.add_argument("--video_dir",
                    default="/tmp/deeprccar",
                    help="Directory to save test video")

args = parser.parse_args()

fourcc = cv2.VideoWriter_fourcc(*'MJPG')
video_writer = cv2.VideoWriter("{}/{}.avi".format(args.video_dir, "test"),
                               fourcc, 20.0, (480, 320))

with open(args.csv_file, 'r') as csv:
    csv.readline() #  skip header row
    for line in csv.readlines():
        (timestep, image_file,
        steering_predicted, steering_expected,
        speed_predicted, speed_expected) = line.strip().split(";")
        timestep = int(timestep)
        steering_predicted = int(steering_predicted)
        steering_expected = int(steering_expected)
        speed_predicted = int(speed_predicted)
        speed_expected = int(speed_expected)

        image = cv2.imread(image_file, cv2.IMREAD_COLOR)
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        image = cv2.resize(image, (480, 320), interpolation=cv2.INTER_CUBIC)

        # Count images
        cv2.putText(image, "Image: {}".format(timestep), (5, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, 255)

        cv2.putText(image, "Steering Command: (predicted, expected)=({},{})".
                    format(steering_predicted, steering_expected), (5, 315),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, 255)

        steering_expected = get_mapped_steering_command(steering_expected)
        cv2.line(image, (240, 300), (240 - steering_expected, 200),
                 (0, 255, 0), 3)

        steering_predicted = get_mapped_steering_command(steering_predicted)
        cv2.line(image, (240, 300), (240 - steering_predicted, 200),
                (0, 0, 255), 3)

        speed_expected = get_mapped_speed_command(speed_expected)
        cv2.line(image, (25, 160), (25, 160 - speed_expected),
                (0, 255, 0), 3)
        speed_predicted = get_mapped_speed_command(speed_predicted)
        cv2.line(image, (50, 160), (50, 160 - speed_predicted),
                (0, 0, 255), 3)

        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        video_writer.write(image)
        cv2.imshow('replay test', image)
        if (cv2.waitKey(0) & 0xFF) == ord('q'):  # Hit `q` to exit
            video_writer.release()
            cv2.destroyAllWindows()
            break
