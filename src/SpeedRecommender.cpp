#include "SpeedRecommender.hpp"
#include "DistanceMeter.hpp"
#include "definitions.hpp"
#include <Arduino.h>

void SpeedRecommender::initialize(const DistanceMeter* distanceMeter)
{
    m_distanceMeter = distanceMeter;
}

float SpeedRecommender::getRecommendedSpeed()
{
    return min(m_distanceMeter->getSpeedHint(), DESIRED_MAX_SPEED);
}
