#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

parser = argparse.ArgumentParser()
parser.add_argument("csv_file")

args = parser.parse_args()

dtype = [
    ("epoch", "int"),
    ("validation", "float"),
    ("training", "float"),
]

data = np.loadtxt(args.csv_file, skiprows=1, delimiter=";", dtype=dtype)

fig, ax = plt.subplots()
ax.plot(data["epoch"], data["training"], label='Training', linestyle='--')
ax.plot(data["epoch"], data["validation"], label='Validation')
ax.legend()
ax.set_xlabel("Epoch")
ax.set_ylabel("Loss")
plt.show()
