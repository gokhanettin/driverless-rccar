#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

parser = argparse.ArgumentParser()
parser.add_argument("csv_file")
parser.add_argument("--y_axis", choices=["steering_cmd", "speed_cmd",
                                         "steering", "speed"],
                    default="steering_cmd",
                    help="y-axis to plot")

args = parser.parse_args()

dtype = [
    ("timestep", "int"),
    ("timestamp", "str"),
    ("imagefile", "str"),
    ("steering_cmd", "int"),
    ("speed_cmd", "int"),
    ("steering", "float"),
    ("speed", "float"),
]

labels = {
    "steering_cmd": "Steering Command",
    "speed_cmd": "Speed Command",
    "steering": "Steering Angle (deg)",
    "speed": "Speed (m/s)",
}


data = np.loadtxt(args.csv_file, skiprows=1, delimiter=";", dtype=dtype)
print("{}: (mean, std) = ({}, {})".format(args.y_axis,
                                          np.mean(data[args.y_axis]),
                                          np.std(data[args.y_axis])))
fig, ax = plt.subplots()
x_axis = range(len(data["timestep"]))
ax.plot(x_axis, data[args.y_axis], color='g')
ax.set_xlabel("Timestep")
ax.set_ylabel(labels[args.y_axis])
plt.show()
