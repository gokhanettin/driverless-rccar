#include <Arduino.h>

class Robot;

class DistanceMeter
{
public:
    DistanceMeter():
    m_index(0),
    m_waypointsLen(0),
    m_car(0UL){}

    void initialize(const Robot* car, const float (*waypoints)[2], int waypointsLen);
    float readCrossTrackDistance();
    boolean goalReached();
private:
    int m_index;
    int m_waypointsLen;
    const float (*m_waypoints)[2];
    const Robot *m_car;
};
