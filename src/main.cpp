#include "definitions.hpp"
#include "Robot.hpp"
#include "SpeedMeter.hpp"
#include "DistanceMeter.hpp"
#include "Pid.hpp"

#include <Servo.h>

Servo speedServo;
Servo steeringServo;
Robot robot;
SpeedMeter speedMeter;
DistanceMeter distanceMeter;
Pid speedPid;
Pid steeringPid;

long currentTime;
long previousTime;
long deltaTime;
int frameCounter;

boolean reached;
float speedDesired;
float speedMeasured;
float speedCommandRaw;
int speedCommand;
float crossTrackDistanceDesired;
float crossTrackDistanceMeasured;
float steering;
int steeringCommand;

const float waypoints[][2] = {
    {0.00f, 0.00f},
    {5.00f, 0.00f},
    {5.00f, 0.30f},
    {0.00f, 0.30f},
};

float mapf(float x, float in_min, float in_max, float out_min, float out_max)
{
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

void setup()
{
    speedServo.attach(HW_SPEED_PIN);
    steeringServo.attach(HW_STEERING_PIN);
    robot.initialize();
    robot.setPose(0.0f, 0.0f, 0.0f);
    speedMeter.initialize();
    distanceMeter.initialize(&robot, waypoints, 4);
    speedPid.initialize(SPEED_P, SPEED_I,
        SPEED_D, SPEED_COMMAND_FORWARD, SPEED_COMMAND_NEUTRAL);
    steeringPid.initialize(STEERING_P, STEERING_I,
        STEERING_D, STEERING_RIGHT, STEERING_LEFT);

    currentTime = micros();
    previousTime = currentTime;
    deltaTime = 0L;
    frameCounter = 0;

    reached = false;
    speedDesired = DESIRED_SPEED;
    speedMeasured = 0.0f;
    speedCommandRaw = 0.0f;
    speedCommand = 0;
    crossTrackDistanceDesired = DESIRED_CROSS_TRACK_DISTANCE;
    crossTrackDistanceMeasured = 0.0f;
    steering = 0.0f;
    steeringCommand = 0;

    Serial.begin(9600);

    steeringServo.writeMicroseconds(STEERING_COMMAND_NEUTRAL);
    speedServo.writeMicroseconds(SPEED_COMMAND_NEUTRAL);
    delay(3000);
}

void loop()
{
    currentTime = micros();
    deltaTime = currentTime - previousTime;
    speedMeter.readRaw();
    if (deltaTime >= 1000) {
        ++frameCounter;
        // 1000 Hz Task
        if (distanceMeter.goalReached()) {
            reached = true;
        }
        robot.updatePose(steering, speedMeasured, currentTime);
        if (frameCounter % 40 == 0) {
            // 25 Hz Task
            speedMeasured = speedMeter.read(currentTime);
            if (reached) {
                speedDesired = 0.0f;
            }
            speedCommandRaw = speedPid.update(speedDesired,
              speedMeasured, currentTime);
            speedCommand  = (int)mapf(speedCommandRaw,
               SPEED_COMMAND_FORWARD, SPEED_COMMAND_NEUTRAL,
               SPEED_COMMAND_NEUTRAL, SPEED_COMMAND_FORWARD);
            speedServo.writeMicroseconds(speedCommand);
        }
        if (frameCounter % 50 == 0) {
            // 20 Hz Task
            crossTrackDistanceMeasured = distanceMeter.readCrossTrackDistance();
            steering = steeringPid.update(crossTrackDistanceDesired,
              crossTrackDistanceMeasured, currentTime);
            steeringCommand = (int)mapf(steering, STEERING_RIGHT, STEERING_LEFT,
                 STEERING_COMMAND_RIGHT, STEERING_COMMAND_LEFT);
            steeringServo.writeMicroseconds(steeringCommand);
        }
        if (frameCounter % 100 == 0) {
            // 10 Hz Task
            Serial.print(speedCommand);
            Serial.print(" ");
            Serial.print(steeringCommand);
            Serial.print(" ");
            Serial.print(speedMeasured);
            Serial.print(" ");
            Serial.print(steering * 180.0f / PI);
            Serial.print(" ");
            Serial.print(crossTrackDistanceMeasured);
            Serial.print(" ");
            Serial.print(robot.x());
            Serial.print(" ");
            Serial.println(robot.y());
        }
        previousTime = currentTime;
    }
    if (frameCounter >= 1000) {
        frameCounter = 0;
    }
}
