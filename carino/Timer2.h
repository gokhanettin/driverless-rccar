#ifndef TIMER2_H
#define TIMER2_H

class TimerTwo
{
public:
    TimerTwo();

    void setup();
    unsigned long micros();
    void reset();
    void interruptOn();
    void interruptOff();
    void countup();


private:
    volatile unsigned long overflow_count_;
    unsigned long total_count_;
};

extern TimerTwo Timer2;

#endif /* TIMER2_H */
