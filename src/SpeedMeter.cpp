#include "SpeedMeter.hpp"
#include "definitions.hpp"

void SpeedMeter::initialize()
{
  m_previousTime = micros();
  m_pulse = 0;
  m_isFalling = false;
  pinMode(HW_SPEED_SENSOR_PIN, INPUT);
}

void SpeedMeter::readRaw()
{
  if (digitalRead(HW_SPEED_SENSOR_PIN) == 0) {
    if (!m_isFalling) {
      m_pulse++;
    }
    m_isFalling = true;
  } else {
      m_isFalling = false;
  }
}

float SpeedMeter::read(long currentTime)
{
    float rotation = m_pulse / (float)ROBOT_SHAFT_PPR;
    m_pulse = 0;
    float dt = (currentTime - m_previousTime) / 1000000.0f;
    m_previousTime = currentTime;
    return rotation * ROBOT_SHAFT_DPR / dt;
}
