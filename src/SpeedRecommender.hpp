class DistanceMeter;

class SpeedRecommender
{
public:
    SpeedRecommender():
    m_distanceMeter(0UL){}

    void initialize(const DistanceMeter* distanceMeter);
    float getRecommendedSpeed();
private:
    const DistanceMeter *m_distanceMeter;
};
