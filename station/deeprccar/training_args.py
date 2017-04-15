#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

def _get_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset_dir",
                        help="Dataset directory")
    parser.add_argument("--checkpoint_dir",
                        default="/tmp/deeprccar",
                        help="Directory to save training files")
    parser.add_argument("--batch_size", type=int, default=4,
                        help="Minibatch size")
    parser.add_argument("--lookback_length", type=int, default=5,
                        help="The number of images from the past")
    parser.add_argument("--sequence_length", type=int, default=10,
                        help="Sequence length for RNNs")
    parser.add_argument("--validation_percentage", type=int, default=10,
                        help="Validation percentage of dataset")
    parser.add_argument("--num_units", type=int, default=32,
                        help="num_units of the LSTMCell")
    parser.add_argument("--num_proj", type=int, default=32,
                        help="num_proj of the LSTMCell")
    parser.add_argument("--clip_norm", type=float, default=15.0,
                              help="Used for gradient clipping")
    parser.add_argument("--learning_rate", type=float, default=1e-4,
                              help="Learning rate")
    parser.add_argument("--keep_prob", type=float, default=0.25,
                              help="Keep probability for dropout layers.")
    parser.add_argument("--autoregressive_weight", type=float, default=0.1,
                              help="Autoregressive contribution to loss")
    parser.add_argument("--ground_truth_weight", type=float, default=0.1,
                              help="Ground truth contribution to loss.")
    parser.add_argument("--num_epochs", type=int, default=100,
                              help="The number of epochs.")
    return parser.parse_args()

args = _get_arguments()
