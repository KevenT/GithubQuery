package com.example.zane.githubquery.inject.module;

import android.text.TextUtils;
import android.util.Log;

import com.ayvytr.okhttploginterceptor.LoggingInterceptor;
import com.ayvytr.okhttploginterceptor.LoggingLevel;
import com.example.zane.githubquery.BuildConfig;
import com.example.zane.githubquery.config.GithubApi;
import com.example.zane.githubquery.inject.qualifier.ForAppInterceptor;
import com.example.zane.githubquery.inject.qualifier.ForNetInterceptor;
import com.example.zane.githubquery.model.bean.data.remote.GithubApiService;
import com.example.zane.githubquery.utils.FileUtils2;
//import com.squareup.okhttp.Cache;
//import com.squareup.okhttp.Interceptor;
//import com.squareup.okhttp.OkHttpClient;
//import com.squareup.okhttp.Request;
//import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import dagger.Module;
import dagger.Provides;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
//import retrofit.GsonConverterFactory;
//import retrofit.Retrofit;
//import retrofit.RxJavaCallAdapterFactory;
//import static com.squareup.okhttp.internal.Internal.logger;
//import static okhttp3.internal.Internal.logger;


/**
 * Created by Zane on 16/1/26.
 */
@Module
public class GithubApiModule {

    //自定义应用拦截器
    @ForAppInterceptor
    @Provides
    Interceptor provicesLoggingIntercepter() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                //1.请求前--打印请求信息
                long t1 = System.nanoTime();
                Log.e("Okhttp==", String.format("Sending request %s on %s%n%s",
                        request.url(), chain.connection(), request.headers()));

                //2.网络请求
                Response response = chain.proceed(request);

                //3.网络响应后--打印响应信息
                long t2 = System.nanoTime();

                Log.e("Okhttp==", String.format("Received response for %s in %.1fms%n%s  =====",
                        response.request().url(), (t2 - t1) / 1e6d,
                        response.headers()
                ));
                // response.body() 只能被第一次
                Log.e("okhttp--",response.body()+"");
                return response;
            }
        };
    }

    //    HttpLoggingInterceptor
    //这行必须加 不然默认不打印
//        logIT.setLevel(HttpLoggingInterceptor.Level.BASIC);
    @Provides
    HttpLoggingInterceptor provicesHttpLoggingIntercepter() {
        return new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                try {
                    String text = URLDecoder.decode(message, "utf-8");
                    Log.e("OKHttp-----", text);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    Log.e("OKHttp-----", message);
                }
            }
        });
    }


    @ForNetInterceptor
    @Provides
    Interceptor providesIntercepter() {
        return new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Request request = chain.request();
                Response response = chain.proceed(request);

                String chainControl = request.cacheControl().toString();
                if (TextUtils.isEmpty(chainControl)) {
                    chainControl = "public, max-age=60, max-stale=240000";
                }

                return response.newBuilder()
                        .addHeader("Chain-Control", chainControl)
                        .removeHeader("Pragma")
                        .build();
            }
        };
    }

    @Provides
    Cache providesCache() {
        File httpCacheFile = FileUtils2.getDiskCacheDir("response");
        return new Cache(httpCacheFile, 1024 * 10 * 1024);
    }

    @Provides
    GithubApiService providesGithubApiService(@ForNetInterceptor Interceptor interceptor,
                                              @ForAppInterceptor Interceptor logIT, Cache cache) {

        LoggingLevel level = LoggingLevel.NONE;
        if (BuildConfig.DEBUG){
            level= LoggingLevel.ALL;
        }
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor(level);

        OkHttpClient mClient = new OkHttpClient.Builder()
                .addInterceptor(logIT) // 拦截器顺序执行拦截
                .addInterceptor(loggingInterceptor)
                .addNetworkInterceptor(interceptor)
                .cache(cache)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GithubApi.githubApi)
                .client(mClient)
                .addConverterFactory(GsonConverterFactory.create())
//                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        return retrofit.create(GithubApiService.class);

    }


}
