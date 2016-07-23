#include "definitions.hpp"
#include "Robot.hpp"
#include "SpeedMeter.hpp"
#include "DistanceMeter.hpp"
#include "SpeedRecommender.hpp"
#include "Pid.hpp"

#include <Servo.h>

Servo speedServo;
Servo steeringServo;
Robot robot;
SpeedMeter speedMeter;
DistanceMeter distanceMeter;
SpeedRecommender speedRecommender;
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

const float waypoints[][3] = {
    {0.00f,  0.00f, 1.50f},
    {2.00f,  0.00f, 0.75f},
    {3.75f,  0.00f, 0.75f},
    {4.63f,  0.25f, 0.75f},
    {5.06f,  0.63f, 1.00f},
    {5.28f,  1.06f, 1.00f},
    {5.40f,  1.53f, 1.00f},
    {5.45f,  2.01f, 1.00f},
    {5.47f,  2.50f, 1.00f},
    {5.50f,  3.00f, 0.00f}
};

// const float waypoints[][3] = {
//     {0.00f,  0.00f, 1.50f},
//     {2.00f,  0.00f, 1.50f},
//     {3.75f,  0.00f, 1.50f},
//     {4.63f,  0.25f, 1.50f},
//     {5.06f,  0.63f, 1.50f},
//     {5.28f,  1.06f, 1.50f},
//     {5.40f,  1.53f, 1.50f},
//     {5.45f,  2.01f, 1.50f},
//     {5.47f,  2.50f, 1.50f},
//     {5.50f,  3.00f, 0.00f}
// };

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
    distanceMeter.initialize(&robot, waypoints, 10);
    speedRecommender.initialize(&distanceMeter);
    speedPid.initialize(SPEED_P, SPEED_I,
        SPEED_D, SPEED_COMMAND_FORWARD, SPEED_COMMAND_NEUTRAL);
    steeringPid.initialize(STEERING_P, STEERING_I,
        STEERING_D, STEERING_RIGHT, STEERING_LEFT);

    currentTime = micros();
    previousTime = currentTime;
    deltaTime = 0L;
    frameCounter = 0;

    reached = false;
    speedDesired = 0.0f;
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
        speedDesired = speedRecommender.getRecommendedSpeed() * (!reached);
        robot.updatePose(steering, speedMeasured, currentTime);
        if (frameCounter % 40 == 0) {
            // 25 Hz Task
            speedMeasured = speedMeter.read(currentTime);
            speedCommandRaw = speedPid.update(speedDesired,
              speedMeasured, currentTime);
            speedCommand  = (int)mapf(speedCommandRaw,
               SPEED_COMMAND_FORWARD, SPEED_COMMAND_NEUTRAL,
               SPEED_COMMAND_NEUTRAL, SPEED_COMMAND_FORWARD);
            speedServo.writeMicroseconds(speedCommand);
        }
        if (frameCounter % 25 == 0) {
            // 40 Hz Task
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
