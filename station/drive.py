#!/usr/bin/env python3
# -*- coding: utf-8 -*-


import socket
import os
import datetime
from collections import deque
import multiprocessing as mp

import numpy as np
import cv2
import tensorflow as tf

from drive_args import args


HOST = "0.0.0.0"
PORT = 5000


def tcp_receive(recv_q, conn):
    timestep = 0
    has_connection = True
    while True:
        timestep += 1
        buff = b''
        while True:
            c = conn.recv(1)
            if c == b'$':
                print("Client asked to disconnect")
                conn.close()
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

        header = buff.decode().split(';')
        speed_cmd = int(header[0])
        steering_cmd = int(header[1])
        speed = float(header[2])
        steering = float(header[3])
        size = int(header[4])

        jpeg = bytearray()
        while size > 0:
            chunk = conn.recv(size)
            jpeg.extend(chunk)
            size -= len(chunk)

        # timestamp in miliseconds
        timestamp = round(datetime.datetime.now().timestamp() * 1000)

        image = cv2.imdecode(np.frombuffer(jpeg, dtype=np.uint8),
                                cv2.IMREAD_COLOR)
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        recv_q.put((image, steering_cmd, speed_cmd, steering, speed,
                    timestep, timestamp))


def predict_commands(recv_q, send_q, monitor_q):
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

        while True:
            received = recv_q.get()
            image = received[0]
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

            # predicted: [steering_cmd, speed_cmd]
            predicted = np.round(prediction).flatten().astype(np.int32)
            send_q.put(predicted)
            monitor_q.put((received, predicted))


def tcp_send(send_q, conn):
    while True:
        predicted = send_q.get()
        commands = "[{};{}]".format(predicted[1], predicted[0])
        conn.sendall(commands.encode())



def main():
    if not os.path.isdir(args.save_dir):
        os.makedirs(args.save_dir)


    fourcc = cv2.VideoWriter_fourcc(*'MJPG')
    video_writer = cv2.VideoWriter("{}/{}.avi"
                                   .format(args.save_dir, "onroad-test"),
                                fourcc, 20.0, (480, 320))

    drive_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    drive_socket.bind((HOST, PORT))
    drive_socket.listen(1)
    print("Waiting for a new connection")
    conn, addr = drive_socket.accept()
    print("Connection from: " + str(addr))
    recv_q = mp.Queue()
    receiver = mp.Process(target=tcp_receive, args=(recv_q, conn))

    send_q = mp.Queue()
    sender = mp.Process(target=tcp_send, args=(send_q, conn))

    monitor_q = mp.Queue()
    predictor = mp.Process(target=predict_commands,
                           args=(recv_q, send_q, monitor_q))

    receiver.start()
    predictor.start()
    sender.start()

    while True:
        received, predicted = monitor_q.get()
        print(received[1:], predicted)
        image = received[0]
        image = cv2.resize(image, (480, 320),
                            interpolation=cv2.INTER_CUBIC)
        # We can draw or write onto the image here

        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        video_writer.write(image)
        cv2.imshow('drive', image)
        if (cv2.waitKey(1) & 0xFF) == ord('q'):  # Hit `q` to exit
            video_writer.release()
            cv2.destroyAllWindows()
            recv_q.close()
            send_q.close()
            monitor_q.close()
            receiver.terminate()
            sender.terminate()
            predictor.terminate()
            conn.close()
            break


if __name__ == '__main__':
    main()
