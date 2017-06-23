package com.google.android.gms.samples.vision.barcodereader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import io.reactivex.Single;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by nhosoya on 2017/06/22.
 */

public class ApiClient {

    private static ApiClient apiClient;
    private YahooApiService yahooApiService;

    public static ApiClient getInstance() {
        if (apiClient != null) {
            return apiClient;
        }

        apiClient = new ApiClient();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        HttpUrl url = request.url().newBuilder().addQueryParameter("appid", BuildConfig.YAHOO_APP_ID).build();
                        request = request.newBuilder().url(url).build();
                        return chain.proceed(request);
                    }
                })
                .build();
        apiClient.yahooApiService = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://shopping.yahooapis.jp")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(createGson()))
                .build()
                .create(YahooApiService.class);

        return apiClient;
    }

    private static Gson createGson() {
        return new GsonBuilder().setDateFormat("yyyy/MM/dd HH:mm:ss").create();
    }

    public Single<ItemDetail> getItemDetail(String janCode) {
        return yahooApiService.getItemDetail(janCode);
    }

    private interface YahooApiService {

        @GET("/ShoppingWebService/V1/json/itemSearch")
        Single<ItemDetail> getItemDetail(@Query("jan") String janCode);
    }

    public class ItemDetail {

        @SerializedName("ResultSet")
        ResultSet resultSet;
    }

    public class ResultSet {
        @SerializedName("0")
        Hoge hoge;
    }

    public class Hoge {
        @SerializedName("Result")
        Result result;
    }

    public class Result {
        @SerializedName("0")
        Item item;
    }

    public class Item {
        @SerializedName("Name")
        String name;
        @SerializedName("Url")
        String url;
        @SerializedName("Image")
        Image image;
    }

    public class Image {
        @SerializedName("Small")
        String smallUrl;
        @SerializedName("Medium")
        String mediumUrl;
    }
}
