#!/usr/bin/env python
# -*- coding: utf-8 -*-

from sys import stdout

# Criticial steering commands measured from the receiver
STEERING_COMMAND_NEUTRAL = 0
STEERING_COMMAND_HALF_RANGE = 1.0
STEERING_COMMAND_RIGHT = STEERING_COMMAND_NEUTRAL - STEERING_COMMAND_HALF_RANGE
STEERING_COMMAND_LEFT = STEERING_COMMAND_NEUTRAL + STEERING_COMMAND_HALF_RANGE

# Critical speed commands measured from the receiver
SPEED_COMMAND_NEUTRAL = 0
SPEED_COMMAND_HALF_RANGE = 24
SPEED_COMMAND_FORWARD = SPEED_COMMAND_NEUTRAL - SPEED_COMMAND_HALF_RANGE
SPEED_COMMAND_BACKWARD = SPEED_COMMAND_NEUTRAL + SPEED_COMMAND_HALF_RANGE


# Utility function to map float `x` from input range to output range.
def map_range(x, in_min, in_max, out_min, out_max):
    y = (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min
    return int(y)


def get_mapped_steering_command(cmd):
    return map_range(cmd, STEERING_COMMAND_RIGHT, STEERING_COMMAND_LEFT,
                     -100, 100)


def get_mapped_speed_command(cmd):
    return map_range(cmd, SPEED_COMMAND_FORWARD, SPEED_COMMAND_BACKWARD,
                     -100, 100)


def progressbar(total, done, prefix="", postfix="", size=60):
    filled = size * done // total
    stdout.write("{prefix}[{sharps}{dots}] {done:04d}/{total:04d}{postfix}\r"
                 .format(prefix=prefix,
                         sharps="#" * filled,
                         dots="." * (size - filled),
                         done=done,
                         total=total,
                         postfix=postfix))
    stdout.flush()

    if total == done:
        stdout.write("\n")
        stdout.flush()



if __name__ == "__main__":
    import time

    for count in range(15):
        progressbar(15, count + 1, "Computing: ", " --> ")
        time.sleep(0.1)  # long computation
