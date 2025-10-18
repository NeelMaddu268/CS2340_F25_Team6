package com.example.sprintproject.model;
import java.util.Locale;


public class AppDate {
    public final int year, month, day;

    public AppDate(int y, int m, int d) { year = y; month = m; day = d; }

    public String toIso() { return String.format(Locale.US,"%04d-%02d-%02d", year, month, day); }


    public static AppDate fromYMD(int y, int m, int d) { return new AppDate(y, m, d); }
}
