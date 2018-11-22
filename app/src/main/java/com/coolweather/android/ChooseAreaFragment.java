package com.coolweather.android;

import android.app.FragmentContainer;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;



import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by user on 18-11-20.
 * 用于遍历省市县数据的碎片
 * 创建视图适配器的一般步骤：
 * 1、创建承载数据的视图容器ListView
 * 2、创建数据源List
 * 3、创建适配器并把数据绑定在适配器
 * 4、将适配器绑定在视图容器
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    //省列表
    private List<Province> provinceList;
    //市列表
    private List<City> cityList;
    //县列表
    private List<County> countyList;
    //选择的省份
    private Province selectedProvince;
    //选择的城市
    private City selectedCity;
    //当前选择的级别
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        //获取到了一些控件的实例
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView)view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        listView = (ListView)view.findViewById(R.id.list_view);
        //初始化ArrayAdapter,并设置为ListView的适配器
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (currentLevel == LEVEL_COUNTY){
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        //加载省级数据
        queryProvinces();
    }

    //查询全国所有的省,优先从数据库查询,如果没有查询到再去服务器上查询
    private void queryProvinces(){
        //将头布局的标题设置为中国
        titleText.setText("中国");
        //隐藏返回按钮,因为省级列表已经不能再返回了
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() > 0 ){
            dataList.clear();
            for(Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    //查询选中省内的所有市,优先从数据库查询,如果没有查询到再去服务器上查询
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0 ){
            dataList.clear();
            for(City city: cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china" + "/" +provinceCode;
            Log.d("TRF1308","provinceCode="+provinceCode+"  ,address="+address);
            queryFromServer(address,"city");
        }
    }
    //查询选中市内的所有县,优先从数据库查询,如果没有查询到再去服务器上查询
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0 ){
            dataList.clear();
            for(County county: countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china" + "/" +  provinceCode + "/" + cityCode;
            queryFromServer(address,"county");
        }
    }

    //根据传入的地址和类型从服务器上查询省市县数据
    private void queryFromServer(String address,final String type){
        showProgressDialog();
        //向服务器发送请求,响应的数据会回调到onResponse()中,然后调用Utility的handlePeovinceResponse()方法来解析和处理服务器返回的数据
        HttpUtil.sendOkHttpRequest(address,new Callback(){
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Log.d("TRF1308","responseText="+responseText);
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handlePeovinceResponse(responseText);
                }else if("city".equals(type)){
//                    Log.d("TRF1308","responseText="+responseText);
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if(result){
                    //queryProvinces()方法牵扯到了UI操作，因此必须要在主线程中调用
                    //借助runOnUiThread()方法来实现从子线程切换到主线程
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                //现在数据库中已经存在了数据,因此调用queryProvinces()就会直接将数据显示到界面上l
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
            @Override
            public void onFailure(Call call,IOException e){
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    //显示进度对话框
    private void showProgressDialog(){
        if(progressDialog ==  null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载......");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }
    //关闭进度对话框
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
