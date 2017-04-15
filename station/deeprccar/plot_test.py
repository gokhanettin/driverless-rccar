#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import csv
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

parser = argparse.ArgumentParser()
parser.add_argument("csv_file")
parser.add_argument("--y_axis", choices=["steering", "speed"],
                    default="steering",
                    help="y-axis is either steering or speed")

args = parser.parse_args()

dtype = [
    ("timestep", "int"),
    ("image_file", "str"),
    ("steering_predicted", "int"),
    ("steering_expected", "int"),
    ("speed_predicted", "int"),
    ("speed_expected", "int"),
]


data = np.loadtxt(args.csv_file, skiprows=1, delimiter=";", dtype=dtype)

fig, ax = plt.subplots()

if  args.y_axis == "steering":
    ax.plot(data["timestep"], data["steering_predicted"], label='Predicted',
            linestyle='--')
    ax.plot(data["timestep"], data["steering_expected"], label='Expected')
    ax.legend()
    ax.set_xlabel("Time")
    ax.set_ylabel("Steering Command")
    print("RMSE: ", np.sqrt(np.mean(np.square(data["steering_predicted"] -
                                              data["steering_expected"]))))
else:
    ax.plot(data["timestep"], data["speed_predicted"], label='Predicted',
            linestyle='--')
    ax.plot(data["timestep"], data["speed_expected"], label='Expected')
    ax.legend()
    ax.set_xlabel("Time")
    ax.set_ylabel("Speed Command")
    print("RMSE: ", np.sqrt(np.mean(np.square(data["speed_predicted"] -
                                              data["speed_expected"]))))

plt.show()
