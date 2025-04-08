package com.example.flighttracker

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.Date
import java.util.concurrent.TimeUnit


object RetrofitClient {
  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
  }
  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30,TimeUnit.SECONDS)
    .build()
  private val moshi = Moshi.Builder()
    .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
//    .add(KotlinJsonAdapterFactory()) // Includes basic types
    .build()

  val instance: FlightApiService by lazy {
    Retrofit.Builder()
      .baseUrl(FlightApiService.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(FlightApiService::class.java)
  }
}