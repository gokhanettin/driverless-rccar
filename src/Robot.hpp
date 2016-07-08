class Robot
{
public:
    Robot ():
    m_x(0.0f),
    m_y(0.0f),
    m_orientation(0.0f),
    m_previousTime(0L){}

    float x() const
    {return m_x;}

    float y() const
    {return m_y;}

    float orientation() const
    {return m_orientation;}

    void setPose(float x, float y, float orientation)
    {m_x = x; m_y = y; m_orientation = orientation;}

    void initialize();

    void updatePose(float steering, float speed, long currentTime);

private:
    float m_x;
    float m_y;
    float m_orientation;
    long  m_previousTime;

};
