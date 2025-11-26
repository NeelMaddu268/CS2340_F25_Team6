// This class stores the year/month/day date. It also has helper methods
// that allow the shifting of dates by days and months and can
// create new AppDate objects.

package com.example.sprintproject.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class AppDate {
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

    public static String addDays(String originalDate, int daysToAdd, int monthsToAdd) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(originalDate);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
            cal.add(Calendar.MONTH, monthsToAdd);
            return sdf.format(cal.getTime());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static AppDate fromYMD(int y, int m, int d) {
        return new AppDate(y, m, d);
    }
}
