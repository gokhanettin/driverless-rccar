#include <Arduino.h>

class SpeedMeter
{
public:
  SpeedMeter():
  m_previousTime(0L){}

  void initialize();
  void readRaw();
  float read(long currentTime);
private:
  long m_previousTime;
  long m_pulse;
  boolean m_isFalling;
};
