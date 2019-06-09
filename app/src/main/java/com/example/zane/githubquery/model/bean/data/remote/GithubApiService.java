package com.example.zane.githubquery.model.bean.data.remote;

import android.graphics.Bitmap;

import com.example.zane.githubquery.model.bean.Repos;
import com.example.zane.githubquery.model.bean.Users;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;


//import retrofit.http.GET;
//import retrofit.http.Path;
//import rx.Observable;

/**
 * Created by Zane on 16/1/26.
 */
public interface GithubApiService {

    @GET("{userName}")
    Observable<Users> getUserInfo(@Path("userName") String userName);

    @GET("{userName}/repos")
    Observable<List<Repos>> getReposInfo(@Path("userName") String userName);

}
