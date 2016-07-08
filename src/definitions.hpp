#ifndef DEFINITIONS_H
#define DEFINITIONS_H

#include <Arduino.h>
// Tolerance in radians for turn calculation, otherwise straight line
#define ROBOT_TURN_TOLERANCE (0.01f)
// Abs max steering angle in radians
#define ROBOT_MAX_STEERING (PI/10.0f)
// Robot length in meter
#define ROBOT_LENGTH (0.286f)
// Displacement per shaft revolution
#define ROBOT_SHAFT_DPR (0.105f)
// Sensor pulses per shaft revolution
#define ROBOT_SHAFT_PPR (8)
// Sensitivity to stop when we are close enough to the goal
#define ROBOT_GOAL_SENSITIVITY (0.2f)

// Shaft is driven by UNO pin 10
#define HW_SPEED_PIN (10)
// Steering servo is driven by UNO pin 9
#define HW_STEERING_PIN (9)
// Speed sensor interrupt is attached to interrupt 0 (pin 2)
#define HW_SPEED_SENSOR_PIN (2)

#define SPEED_COMMAND_TRIM        (0)
#define SPEED_COMMAND_BACKWARD    (2000 + SPEED_COMMAND_TRIM)
#define SPEED_COMMAND_NEUTRAL     (1500 + SPEED_COMMAND_TRIM)
#define SPEED_COMMAND_FORWARD     (1000 + SPEED_COMMAND_TRIM)

#define STEERING_COMMAND_TRIM     (68)
#define STEERING_COMMAND_LEFT     (2000 + STEERING_COMMAND_TRIM)
#define STEERING_COMMAND_NEUTRAL  (1500 + STEERING_COMMAND_TRIM)
#define STEERING_COMMAND_RIGHT    (1000 + STEERING_COMMAND_TRIM)

// SPEED PID values
#define SPEED_P (5.42539876f)
#define SPEED_I (31.29200024f)
#define SPEED_D (0.0f)

// Steering PID values
#define STEERING_P (5.93277065f)
#define STEERING_I (7.69425523f)
#define STEERING_D (5.75460786f)
#define STEERING_LEFT (ROBOT_MAX_STEERING)
#define STEERING_RIGHT (-ROBOT_MAX_STEERING)

// Desired values
#define DESIRED_SPEED (1.0f)
#define DESIRED_CROSS_TRACK_DISTANCE (0.0f)
#endif
