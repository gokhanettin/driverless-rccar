#include "definitions.hpp"
#include "DistanceMeter.hpp"
#include "Robot.hpp"

void DistanceMeter::initialize(const Robot* car, const float (*waypoints)[2],
int waypointsLen)
{
    m_car = car;
    m_waypoints = waypoints;
    m_waypointsLen = waypointsLen;
    m_index = 0;
}

float DistanceMeter::readCrossTrackDistance()
{
    float x = m_car->x();
    float y = m_car->y();
    const float (*p)[2] = m_waypoints;
    int i = m_index;

    float p0p[] = {x - p[i][0], y - p[i][1]};
    float p0p1[] = {p[i+1][0] - p[i][0], p[i+1][1] - p[i][1]};
    float p0p1LenSquared = p0p1[0] * p0p1[0] + p0p1[1] * p0p1[1];

    float b = (p0p[0]*p0p1[0] + p0p[1]*p0p1[1]) / p0p1LenSquared;
    float c = (p0p1[0]*p0p[1] - p0p1[1]*p0p[0]) / sqrt(p0p1LenSquared);

    if (b > 1.0f && i < (m_waypointsLen - 2)) {
        ++m_index;
    }
    if (m_index == m_waypointsLen - 1) {
        c = 0.0f;
    }
    return c;
}

boolean DistanceMeter::goalReached()
{
    const float *goal = m_waypoints[m_waypointsLen - 1];
    float x = m_car->x();
    float y = m_car->y();
    float dx = goal[0] - x;
    float dy = goal[1] - y;
    float d =  sqrt(dx * dx + dy * dy);
    return d < ROBOT_GOAL_SENSITIVITY;
}
