package com.example.sprintproject.model;
import java.util.Locale;


public class AppDate {
    private final int year;
    private final int month;
    private final int day;

    public AppDate(int y, int m, int d) {
        year = y;
        month = m;
        day = d;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public String toIso() {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }

    public static AppDate fromYMD(int y, int m, int d) {
        return new AppDate(y, m, d);
    }
}
