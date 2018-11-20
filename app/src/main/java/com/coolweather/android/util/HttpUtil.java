package com.coolweather.android.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by user on 18-11-20.
 * 全国所有省市县的数据都是从服务器端获取的
 * 因此这里用于和服务器交互
 */

public class HttpUtil {
    //发起HTTP请求,传入请求地址,并注册一个回调来处理服务器响应
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
