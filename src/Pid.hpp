class Pid
{
public:
  Pid():
  m_p(0.0f), m_i(0.0f), m_d(0.0f),
  m_minout(0.0f), m_maxout(0.0f),
  m_sumerr(0.0f), m_preverr(0.0f){}

  void initialize(float p, float i, float d, float minout, float maxout);
  float update(float desired, float measured, long currentTime);
private:
  long m_previousTime;
  float m_p;
  float m_i;
  float m_d;
  float m_minout;
  float m_maxout;
  float m_sumerr;
  float m_preverr;
};
