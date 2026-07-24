package com.demetrius.f1glyph.data

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val moshi: Moshi = Moshi.Builder().build()

    private fun client(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val jolpicaApi: JolpicaApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.jolpi.ca/ergast/f1/")
            .client(client())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JolpicaApi::class.java)
    }

}
