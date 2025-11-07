package com.example.sprintproject.model;
import java.util.Locale;


public class AppDate {
//    private final int year;
//    private final int month;
//    private final int day;
    private int year;
    private int month;
    private int day;

    public AppDate() {

    }

    public AppDate(int y, int m, int d) {
        year = y;
        month = m;
        day = d;
    }

    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }
    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }
    public void setDay(int day) {
        this.day = day;
    }


    public String toIso() {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    public static AppDate fromYMD(int y, int m, int d) {
        return new AppDate(y, m, d);
    }
}
