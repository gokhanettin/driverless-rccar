#include <Arduino.h>

class Robot;

class DistanceMeter
{
public:
    DistanceMeter():
    m_index(0),
    m_waypointsLen(0),
    m_robot(0UL){}

    void initialize(const Robot* robot, const float (*waypoints)[3], int waypointsLen);
    float readCrossTrackDistance();
    boolean goalReached() const;
    float getSpeedHint() const;
private:
    int m_index;
    int m_waypointsLen;
    const float (*m_waypoints)[3];
    const Robot *m_robot;
};
