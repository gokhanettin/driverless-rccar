#include "Robot.hpp"
#include "definitions.hpp"

void Robot::initialize()
{
    m_previousTime = micros();
}

void Robot::updatePose(float steering, float speed, long currentTime)
{
    float distance = speed * (currentTime - m_previousTime)/1000000.0f;
    m_previousTime = currentTime;
    distance = constrain(distance, 0.0f, distance);
    steering = constrain(steering, -ROBOT_MAX_STEERING, ROBOT_MAX_STEERING);

    float turn = tan(steering) * distance / ROBOT_LENGTH;
    m_orientation += turn;
    while (m_orientation > TWO_PI) {
        m_orientation -= TWO_PI;
    }
    while (m_orientation < -TWO_PI) {
        m_orientation += TWO_PI;
    }

    if (abs(turn) < ROBOT_TURN_TOLERANCE) {
        // approximate by straight line motion
        m_x += (distance * cos(m_orientation));
        m_y += (distance * sin(m_orientation));
    } else {
        // approximate bicycle model for motion
        float radius = distance / turn;
        float cx = m_x - (sin(m_orientation) * radius);
        float cy = m_y + (cos(m_orientation) * radius);
        m_x = cx + (sin(m_orientation) * radius);
        m_y = cy - (cos(m_orientation) * radius);
    }
}
