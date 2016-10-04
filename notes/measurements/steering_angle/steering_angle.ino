#include <Servo.h>

#define STEERING_COMMAND_TRIM     (68)
#define STEERING_COMMAND_LEFT     (2000 + STEERING_COMMAND_TRIM)
#define STEERING_COMMAND_NEUTRAL  (1500 + STEERING_COMMAND_TRIM)
#define STEERING_COMMAND_RIGHT    (1000 + STEERING_COMMAND_TRIM)

Servo steeringServo;
long width, value;
void setup()
{
  steeringServo.attach(9);
  width = STEERING_COMMAND_NEUTRAL;
  value = width;
  Serial.begin(9600);
  steeringServo.writeMicroseconds(width);
  delay(4000);
}

void loop()
{
  value = Serial.parseInt();
  if (value != 0) {
    // Valid width entered
    width = value;
  }
  steeringServo.writeMicroseconds(width);
}
