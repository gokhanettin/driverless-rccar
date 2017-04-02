#include <Servo.h>

// Communication modes MOnitor, COntrol, or NOne until requested.
#define MO_COMM          ('M')
#define CO_COMM          ('C')
#define NO_COMM          ('N')

// Commands to control the rc car
#define SPEED_COMMAND    (0)
#define STEERING_COMMAND (1)
#define NUM_COMMANDS     (2)

// UNO pins for speed and steering commands. D10 and D9, respectively.
#define PIN_SPEED_COMMAND    (10)
#define PIN_STEERING_COMMAND (9)

// Displacement per shaft revolution (meter)
#define CAR_SHAFT_DPR (0.095f)

// Sensor pulses per shaft revolution
#define CAR_SHAFT_PPR (8)

// Max possible steering angle (degree)
#define CAR_MAX_STEERING   (18.0f)
#define CAR_STEERING_LEFT  (CAR_MAX_STEERING)
#define CAR_STEERING_RIGHT (-CAR_MAX_STEERING)

// Critical speed commands measured from the receiver
#define SPEED_COMMAND_TRIM       (0)
#define SPEED_COMMAND_BACKWARD   (2000 + SPEED_COMMAND_TRIM)
#define SPEED_COMMAND_NEUTRAL    (1500 + SPEED_COMMAND_TRIM)
#define SPEED_COMMAND_FORWARD    (1000 + SPEED_COMMAND_TRIM)

// Criticial steering commands measured from the receiver
#define STEERING_COMMAND_TRIM    (68)
#define STEERING_COMMAND_LEFT    (2000 + STEERING_COMMAND_TRIM)
#define STEERING_COMMAND_NEUTRAL (1500 + STEERING_COMMAND_TRIM)
#define STEERING_COMMAND_RIGHT   (1000 + STEERING_COMMAND_TRIM)

// These are sent to Android
int speedCommand;
int steeringCommand;
float speed;
float steering;

// Commands come from either Android through bluetooth or the RC receiver
int serial[NUM_COMMANDS];
volatile int receiver[NUM_COMMANDS];

// Interrupt timing
unsigned long interruptTime;

// RC receiver interrupt handling variables
unsigned long lastRisingTime[NUM_COMMANDS];
byte lastLevel[NUM_COMMANDS];

// Wheel encoder for speed measurement
volatile unsigned long pulse;
boolean wasHigh;
unsigned long lastReadTime;

// Main loop control variables
unsigned long currentTime;
unsigned long previousTime;
unsigned long deltaTime;
int frameCounter;

// Variables for handling serial communication
char mode;
char buffer[30];
int index;
boolean valid;

// Servo instances
Servo speedServo;
Servo steeringServo;


/* Utility function to map float `x` from input range to output range. */
float mapf(float x, float in_min, float in_max, float out_min, float out_max)
{
    return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

/* Parses communication mode and serial commands. */
void parse()
{
    char *p = buffer;
    char cmd[10];
    int i = 0;
    int j = 0;

    if (*p == MO_COMM || *p == CO_COMM || *p == NO_COMM) {
        mode = *p;
        return;
    }
    while (true) {
        if (*p == ';') {
            serial[j++] = atof(cmd);
            i = 0;
        } else if (*p == '\0') {
            serial[j] = atof(cmd);
            break;
        } else {
            cmd[i++] = *p;
            cmd[i] = '\0';
        }
        p++;
    }
}

/* Reads valid commands or communication mode request. */
void serialRead()
{
    int c = 0;
    while (Serial.available() > 0) {
        c = Serial.read();
        if (c == '[') {
            valid = true;
            index = 0;
            continue;
        }
        if (valid) {
            if (c == ']') {
                buffer[index] = '\0';
                valid = false;
                parse();
                break;
            } else {
                buffer[index++] = (char)c;
            }
        }
    }
}

/* Sends speed, steering angle and their corresponding recent commands */
void serialWrite()
{
    Serial.print("[" + String(speedCommand)    + ";" +
                       String(steeringCommand) + ";" +
                       String(speed, 2)        + ";" +
                       String(steering, 2)     + "]");
}

/* Returns approx. speed of the car in m/s. */
float getSpeed()
{
    float rotation, dt;
    rotation = pulse / (float)CAR_SHAFT_PPR;
    pulse = 0;
    dt = (currentTime - lastReadTime) / 1000000.0f;
    lastReadTime = currentTime;
    return rotation * CAR_SHAFT_DPR / dt;
}

/* Returns approx. steering angle in degrees. */
float getSteering()
{
    return mapf(steeringCommand, STEERING_COMMAND_RIGHT, STEERING_COMMAND_LEFT,
    CAR_STEERING_RIGHT, CAR_STEERING_LEFT);
}

void setup()
{
    speedCommand = 0;
    steeringCommand = 0;
    speed = 0.0f;
    steering = 0.0f;

    mode = NO_COMM;
    index = 0;
    valid = false;

    deltaTime = 0UL;
    frameCounter = 0;
    currentTime = micros();
    previousTime = currentTime;

    pulse = 0;
    wasHigh = false;
    lastReadTime = currentTime;

    speedServo.attach(PIN_SPEED_COMMAND);
    steeringServo.attach(PIN_STEERING_COMMAND);
    Serial.begin(9600);

    steeringServo.writeMicroseconds(STEERING_COMMAND_NEUTRAL);
    speedServo.writeMicroseconds(SPEED_COMMAND_NEUTRAL);
    delay(3000);

    PCIFR |= (1 << PCIE1);
    PCICR |= (1 << PCIE1);
    PCMSK1 |= (1 << PCINT0); //A0
    PCMSK1 |= (1 << PCINT1); //A1
    PCMSK1 |= (1 << PCINT2); //A2
}

void loop()
{
    currentTime = micros();
    deltaTime = currentTime - previousTime;

    if (deltaTime >= 1000) {
        ++frameCounter;
        // 1000 Hz Task

        if (frameCounter % 40 == 0) {
            // 25 Hz Task
            serialRead();
            if (mode == CO_COMM) {
                speedCommand = serial[SPEED_COMMAND];
                steeringCommand = serial[STEERING_COMMAND];
            } else if (mode == MO_COMM){
                speedCommand = receiver[SPEED_COMMAND];
                steeringCommand = receiver[STEERING_COMMAND];
            } else {
                speedCommand = SPEED_COMMAND_NEUTRAL;
                steeringCommand = STEERING_COMMAND_NEUTRAL;
            }
            speedServo.writeMicroseconds(speedCommand);
            steeringServo.writeMicroseconds(steeringCommand);
        }
        if (frameCounter % 50 == 0) {
            // 20 Hz Task
            speed = getSpeed();
            steering = getSteering();
            if (mode != NO_COMM) {
                serialWrite();
            }
        }
        previousTime = currentTime;
    }
    if (frameCounter >= 1000) {
        frameCounter = 0;
    }
}

// ISR (PCINT0_vect) pin change interrupt for D8 to D13
// ISR (PCINT1_vect) pin change interrupt for A0 to A5
// ISR (PCINT2_vect) pin change interrupt for D0 to D7
// PINB (digital pin 8 to 13)
// PINC (analog input pins)
// PIND (digital pins 0 to 7)

// A0 - CH2 of my receiver - throttle
// A1 - CH1 of my receiver - steering
// A2 - Wheel encoder
ISR(PCINT1_vect) {
    interruptTime = micros();

    // Speed command (aka throttle)
    if ((lastLevel[SPEED_COMMAND] == LOW) && (PINC & B00000001)) {
        lastLevel[SPEED_COMMAND] = HIGH;
        lastRisingTime[SPEED_COMMAND] = interruptTime;
    } else if ((lastLevel[SPEED_COMMAND] == HIGH) && !(PINC & B00000001)) {
        lastLevel[SPEED_COMMAND] = LOW;
        receiver[SPEED_COMMAND] = interruptTime - lastRisingTime[SPEED_COMMAND];
    }

    // Steering command
    if ((lastLevel[STEERING_COMMAND] == LOW) && (PINC & B00000010)){
        lastLevel[STEERING_COMMAND] = HIGH;
        lastRisingTime[STEERING_COMMAND] = interruptTime;
    } else if ((lastLevel[STEERING_COMMAND] == HIGH) && !(PINC & B00000010)) {
        lastLevel[STEERING_COMMAND] = LOW;
        receiver[STEERING_COMMAND] = interruptTime - lastRisingTime[STEERING_COMMAND];
    }

    // Wheel encoder
    if (!(PINC & B00000100)) {
        if (wasHigh) {
            ++pulse;
        }
        wasHigh = false;
    } else {
        wasHigh = true;
    }
}

