#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import csv
from collections import deque

import numpy as np
import cv2

import tensorflow as tf

import dataset as ds
from test_args import args
from utils import progressbar


def do_test():
    test_set = ds.parse_test_dataset(args.dataset_dir)
    print("Test Set Length: {}".format(len(test_set)))

    if not os.path.isdir(args.save_dir):
        os.makedirs(args.save_dir)

    print("Writing test results to ", args.save_dir)

    csv_file = open("{}/{}.csv".format(args.save_dir, "test"), "w")
    csv_writer = csv.writer(csv_file, delimiter=';')
    csv_writer.writerow(("timestep", "image_file",
                         "steering_predicted", "steering_expected",
                         "speed_predicted", "speed_expected"))
    csv_file.flush()

    image_queue = deque()
    next_state = None
    with tf.Session() as sess:
        saver = tf.train.import_meta_graph(args.metagraph_file)
        ckpt = tf.train.latest_checkpoint(args.checkpoint_dir)
        saver.restore(sess, ckpt)
        input_images = tf.get_collection("input_images")[0]
        prev_state = tf.get_collection("prev_state")
        next_state_op = tf.get_collection("next_state")
        prediction_op = tf.get_collection("predictions")[0]
        lookback_length_op = tf.get_collection("lookback_length")[0]
        stats_op = tf.get_collection("stats")

        total_steps = len(test_set)
        for step, sample in enumerate(test_set):
            imfile, expected = sample

            image = cv2.imread(imfile, cv2.IMREAD_COLOR)
            image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

            if len(image_queue) == 0:
                lookback_length = sess.run(lookback_length_op)
                image_queue.extend([image] * (lookback_length + 1))
                mean, stddev = sess.run(stats_op)
                print("Training mean: {}\nTraining stddev: {}"
                      .format(mean, stddev))
            else:
                image_queue.popleft()
                image_queue.append(image)

            image_sequence = np.stack(image_queue)
            feed_dict = {
                input_images: image_sequence,
            }

            if next_state is not None:
                feed_dict.update(dict(zip(prev_state, next_state)))

            next_state, prediction = sess.run([next_state_op,
                                              prediction_op],
                                              feed_dict=feed_dict)

            predicted = np.round(prediction).flatten().astype(np.int32)
            expected = np.array(expected, dtype=np.int32)
            csv_writer.writerow((step, imfile, predicted[0], expected[0],
                                 predicted[1], expected[1]))
            csv_file.flush()
            progressbar(total_steps, step + 1, "Testing ")
        csv_file.close()

do_test()
