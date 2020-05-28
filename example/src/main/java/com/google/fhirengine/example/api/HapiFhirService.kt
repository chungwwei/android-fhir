/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.fhirengine.example.api

import ca.uhn.fhir.parser.IParser
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.hl7.fhir.r4.model.Bundle
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * hapi.fhir.org API communication via Retrofit
 */
interface HapiFhirService {

    @GET("baseR4/Patient")
    suspend fun getPatients(
      @Query("address-country") country: String
    ): Bundle

    @GET("baseR4")
    suspend fun getMorePages(
      @Query("_getpages") pagesId: String,
      @Query("_getpagesoffset") pagesOffset: Int,
      @Query("_getpagescount") pagesCount: Int,
      @Query("_bundleType") bundleType: String
    ): Bundle

    companion object {
        private const val BASE_URL = "https://hapi.fhir.org/"

        fun create(parser: IParser): HapiFhirService {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder()
                    .addInterceptor(logger)
                    .build()
            return Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(FhirConverterFactory(parser))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(HapiFhirService::class.java)
        }
    }
}
