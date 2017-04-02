#include "Timer2.h"
#include <Arduino.h>

TimerTwo Timer2;

TimerTwo::TimerTwo()
    :overflow_count_(0UL), total_count_(0UL)
{
}

void TimerTwo::setup()
{
    // div 8 so the period is 0.5us
    TCCR2B = (TCCR2B & B11111000) | B00000010;

    // We are in normal mode
    TCCR2A &= 0b11111100;
    TCCR2B &= 0b11110111;

    // Enable Timer2 overflow interrupt
    TIMSK2 |= 0b00000001;
}

unsigned long TimerTwo::micros()
{
    byte SREG_save = SREG;            // Back up status register
    noInterrupts();                   // Enter critical section
    byte TCNT2_save = TCNT2;          //Get counter value from Timer 2
    boolean flag = bitRead(TIFR2, 0); // Get Timer 2 overflow flag
    if (flag) {
        /* Special overflow case when interrupts disabled */
        TCNT2_save = TCNT2; // Reread the counter value from Timer 2
        ++overflow_count_;  // Manually increase overflow counter
        TIFR2 |= B00000001; // We manually incremented, no interrupt again
    }
    total_count_ = (overflow_count_ << 8) + TCNT2_save; // x*256 + y
    SREG = SREG_save; // Exit critical section
    return (total_count_ >> 1);
}

void TimerTwo::reset()
{
    overflow_count_ = 0UL; // reset overflow counter
    total_count_ = 0UL;    // reset total counter
    TCNT2 = 0x00;          // reset Timer2 counter
    TIFR2 |= B00000001;    // No immediate overflow interrupt since TCNT2 = 0
}

void TimerTwo::interruptOn()
{
    TIMSK2 &= B11111110;
}

void TimerTwo::interruptOff()
{
    TIMSK2 |= B00000001;
}

void TimerTwo::countup()
{
    ++overflow_count_;
}

ISR(TIMER2_OVF_vect)
{
    Timer2.countup();
}
