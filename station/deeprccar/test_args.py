#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

def _get_arguments():
    parser = argparse.ArgumentParser()
    parser.add_argument("dataset_dir",
                        help="Dataset directory")
    parser.add_argument("checkpoint_dir",
                        help="Directory to restore the params from")
    parser.add_argument("metagraph_file",
                        help="File to import the graph from")
    parser.add_argument("--save_dir",
                        default="/tmp/deeprccar",
                        help="Directory to save test results")
    return parser.parse_args()

args = _get_arguments()
