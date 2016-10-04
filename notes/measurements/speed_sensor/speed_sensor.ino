#include <Servo.h>

#define SPEED_COMMAND_TRIM        (0)
#define SPEED_COMMAND_BACKWARD    (2000 + SPEED_COMMAND_TRIM)
#define SPEED_COMMAND_NEUTRAL     (1500 + SPEED_COMMAND_TRIM)
#define SPEED_COMMAND_FORWARD     (1000 + SPEED_COMMAND_TRIM)

// Displacement per shaft revolution
#define ROBOT_SHAFT_DPR (0.095f)
// Sensor pulse per shaft revolution
#define ROBOT_SHAFT_PPR (8)

#define INPUT_PIN (2)

long pulse;
boolean isFallingEdge;
long previousPulse;
long previousTime;
long previousMeasurementTime;
long currentTime;
int frameCounter;
long deltaTime;
float speedMeasured;
int speedCommand;
boolean shouldWait;

enum State {
  FORWARD,
  BACKWARD,
  DONE
}state;

Servo speedServo;

void setup()
{
  Serial.begin(9600);
  pinMode(INPUT_PIN, INPUT);
  speedServo.attach(10);
  speedCommand = SPEED_COMMAND_NEUTRAL;
  speedServo.writeMicroseconds(speedCommand);
  delay(5000); // Send neutral for 5 sec.

  pulse = 0;
  previousPulse = 0;
  previousTime = micros();
  previousMeasurementTime = currentTime = previousTime;
  deltaTime = 0;
  frameCounter = 0;
  speedMeasured = 0.0f;
  state = FORWARD;
  shouldWait = false;
  isFallingEdge = false;
}


void loop()
{
    currentTime = micros();
    deltaTime = currentTime - previousTime;
    countPulses();
    if (deltaTime >= 1000) {
        ++frameCounter;
        // 1000 Hz Task
        if (frameCounter % 40 == 0 && state != DONE) {
            // 25 Hz Task
            previousPulse = pulse;
            Serial.print(speedCommand);
            Serial.print(" ");
            speedMeasured = readSpeed();
            Serial.print(speedMeasured);
            Serial.print(" ");
            Serial.println(previousPulse);
            if (speedCommand > SPEED_COMMAND_FORWARD && state == FORWARD) {
              speedCommand -= 5;
            } else if (speedCommand == SPEED_COMMAND_FORWARD && state == FORWARD) {
              speedCommand = SPEED_COMMAND_NEUTRAL;
              state = BACKWARD;
              shouldWait = true;
            } else if (speedCommand < SPEED_COMMAND_BACKWARD && state == BACKWARD) {
              speedCommand += 5;
            } else if (speedCommand == SPEED_COMMAND_BACKWARD && state == BACKWARD) {
              speedCommand = SPEED_COMMAND_NEUTRAL;
              state = DONE;
            }
            speedServo.writeMicroseconds(speedCommand);
            if (shouldWait) {
              // Wait for zero forward speed for backward speed measurement
              delay(5000);
              shouldWait = false;
            }
        }
        previousTime = currentTime;
    }
    if (frameCounter >= 1000) {
        frameCounter = 0;
    }
}

void countPulses()
{
   if (digitalRead(INPUT_PIN) == 0) {
     if (!isFallingEdge) {
       if (state == FORWARD) ++pulse;
        else if (state == BACKWARD) --pulse;
     }
     isFallingEdge = true; 
   } else {
     isFallingEdge = false;
   }
}

float readSpeed()
{
    float rotation = pulse / (float)ROBOT_SHAFT_PPR;
    float dt = (currentTime - previousMeasurementTime) / 1000000.0f;
    previousMeasurementTime = currentTime;
    pulse = 0;
    return rotation * ROBOT_SHAFT_DPR / dt;
}
