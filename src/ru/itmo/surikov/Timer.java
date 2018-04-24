package ru.itmo.surikov;


import java.util.Date;

public class Timer {
    Date beginDate = new Date();

    public Timer() {

    }

    public String getTime() {
        long delta = new Date().getTime() - beginDate.getTime();
        delta = delta / 1000;

        int min = (int) ((delta / 60));
        int sec = (int) (((delta % 60)));

        if ((min > 0)) {
            return min + " мин. " + sec + " сек.";

        } else return delta + " сек.";

    }

    public long getSeconds() {
        return (new Date().getTime() - beginDate.getTime()) / 1000;
    }
}