#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import numpy as np

DATASET_FILE = "dataset.txt"
IMAGE_FOLDER = "im"
DATASET_FILE_HEADER = "timestep;timestamp;imagefile;steering_cmd;speed_cmd;steering;speed".split(";")

COMMAND_NAMES = DATASET_FILE_HEADER[3:5]  # steering_cmd, speed_cmd
NUM_COMMANDS = len(COMMAND_NAMES)

HEIGHT = 480
WIDTH = 640
CHANNELS = 3  # RGB


class BatchIter:
    """Iterator for looping over the batches given a dataset."""
    def __init__(self, dataset, batch_size, lookback_length, sequence_length):
        self._dataset = dataset
        self._bsize = batch_size
        self._lblen = lookback_length
        self._sqlen = sequence_length
        self._chunk_size = 1 + (len(self._dataset) - 1) // self._bsize
        self._sequence_indices = None
        self._next_count = None

    def __iter__(self):
        self._sequence_indices = [(i * self._chunk_size) % len(self._dataset)
                                  for i in range(self._bsize)]
        self._next_count = 0
        return self

    def __next__(self):
        if len(self) == self._next_count:
            raise StopIteration

        self._next_count += 1
        dataset_len = len(self._dataset)
        raw_batch = [None] * self._bsize

        # Iterate over batch and sequence indices
        for b_idx, f_idx in enumerate(self._sequence_indices):
            lookback = self._dataset[f_idx - self._lblen:f_idx]
            if len(lookback) < self._lblen:
                padding = (self._lblen - len(lookback))
                lookback = [self._dataset[0]] * padding + lookback
            remaining_len = dataset_len - f_idx
            if remaining_len >= self._sqlen:
                sequence = self._dataset[f_idx:f_idx + self._sqlen]
            else:
                sequence = self._dataset[f_idx:] + \
                        self._dataset[:self._sqlen - remaining_len]

            self._sequence_indices[b_idx] = ((f_idx + self._sqlen) %
                                             dataset_len)
            images, commands = zip(*sequence)
            lookback_images, _ = zip(*lookback)
            raw_batch[b_idx] = (np.stack(lookback_images + images),
                                np.stack(commands))
        images, commands = zip(*raw_batch)
        batch = [np.stack(images), np.stack(commands)]
        # Example images/commands indices for dataset length of 22
        #                0  1  2  3  4  5  6  7  8  9
        #                6  7  8  9  10 11 12 13 14 15
        #                12 13 14 15 16 17 18 19 20 21
        #                18 19 20 21 0  1  2  3  4  5
        # ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^  ^
        # 0  0  0  0  0  0  1  2  3  4  5  6  7  8  9
        # 1  2  3  4  5  6  7  8  9  10 11 12 13 14 15
        # 7  8  9  10 11 12 13 14 15 16 17 18 19 20 21
        # 13 14 15 16 17 18 19 20 21 0  1  2  3  4  5
        # images size:  batch_size x (lookback_length + sequence_length)
        # commands size: batch_size x sequence_length x NUM_COMMANDS
        return batch

    def __len__(self):
        return 1 + (self._chunk_size - 1) // self._sqlen


def parse_training_dataset(dataset_dir, sequence_length, batch_size,
                           validation_percentage=10):
    sum_f = np.float128([0.0] * NUM_COMMANDS)
    sum_sq_f = np.float128([0.0] * NUM_COMMANDS)
    rows = _parse_dataset(dataset_dir)
    training_set = []
    validation_set = []
    sequence_x_batch = sequence_length * batch_size
    for index, row in enumerate(rows):
        split = index % (sequence_x_batch * 100)
        if split < sequence_x_batch * (100 - validation_percentage):
            training_set.append(row)
            sum_f += row[1]
            sum_sq_f += row[1] * row[1]
        else:
            validation_set.append(row)
    mean = sum_f / len(training_set)
    variance = sum_sq_f / len(training_set) - mean * mean
    stdev = np.sqrt(variance)
    # We need (mean, stdev) of steering and speed commands to normalize them
    return (training_set, validation_set), (mean, stdev)


def parse_test_dataset(dataset_dir):
    return _parse_dataset(dataset_dir)


def _parse_dataset(dataset_dir):
    filename = dataset_dir + "/" + DATASET_FILE
    with open(filename, 'r') as dsf:
        lines = [line.strip().split(";")[2:5] for line in dsf.readlines()]
        image_folder = dataset_dir + "/" + IMAGE_FOLDER + "/"
        dataset = map(lambda x: (image_folder + x[0],
                                 np.float32(x[1:])), lines)
        # [(image, [steering_cmd, speed_cmd]),
        #  (image, [steering_cmd, speed_cmd]),
        #                                 ...]
        return list(dataset)


if __name__ == "__main__":
    from training_args import args

    (training_set, validation_set), (training_mean, training_stddev) = (
        parse_training_dataset(args.dataset_dir, args.sequence_length,
                               args.batch_size))
    print("(Training Set Mean: {}, Training Set Standard Deviation: {}"
          .format(training_mean, training_stddev))
    print("Training set length: {}".format(len(training_set)))
    print("Validation set length: {}".format(len(validation_set)))
    validation_iter = BatchIter(validation_set, args.batch_size,
                                args.lookback_length, args.sequence_length)
    print("The number of validation batches: {}"
          .format(len(validation_iter)))
    for index, batch in enumerate(validation_iter):
        print(batch)
        if index == 1:
            break
