
#include "Pid.hpp"
#include <Arduino.h>

void Pid::initialize(float p, float i, float d, float minout, float maxout)
{
    m_previousTime = micros();
    m_p = p;
    m_i = i;
    m_d = d;
    m_minout = minout;
    m_maxout = maxout;
    m_sumerr = 0.0f;
    m_preverr = 0.0f;
}

float Pid::update(float desired, float measured, long currentTime)
{
  float err = desired - measured;
  float dt = (currentTime - m_previousTime)/1000000.0f;
  m_previousTime = currentTime;
  m_sumerr += m_i * err * dt;
  m_sumerr = constrain(m_sumerr, m_minout, m_maxout);
  float pterm = m_p * err;
  float iterm = m_sumerr;
  float dterm = m_d * (err - m_preverr) / dt;
  m_preverr = err;
  float out = pterm + iterm + dterm;
  out = constrain(out, m_minout, m_maxout);
  return out;
}
