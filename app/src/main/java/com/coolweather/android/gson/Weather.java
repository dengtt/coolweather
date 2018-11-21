package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by user on 18-11-21.
 * 创建一个总的实体类来引用各个实体类
 */

public class Weather {
    public String status;
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestion suggestion;

    //daily_forecast中包含的是一个数组,因此用List结合
    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}
