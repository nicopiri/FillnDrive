package com.example.fillndrive;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public class DateUtility {
    public static int getCurrentDay(Context context) {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public static int getLastDay(Context context) {
        SharedPreferences settings = getSharedPreferences(context);
        return settings.getInt("day", 0);
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        // Accede alle preferenze condivise
        return context.getSharedPreferences("PREFS", 0);
    }
}
