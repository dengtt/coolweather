package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by user on 18-11-21.
 */

public class Basic {
    //由于JSON中的一些字段可能不太适合直接作为Java字段来命名,这里使用@SerializedName注解的方式来让JSON字段和Java字段之间建立联系
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }

}
