package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface EchoChatApi {

    @GET
    suspend fun getMessages(@Url url: String): List<MessageRaw>

    @POST
    @FormUrlEncoded
    suspend fun login(
        @Url url: String,
        @Field("mode") mode: String = "login",
        @Field("email") email: String,
        @Field("password") oldPassword: String
    ): LoginResponse

    @POST
    @FormUrlEncoded
    suspend fun register(
        @Url url: String,
        @Field("mode") mode: String = "register",
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("password") oldPassword: String,
        @Field("photoUrl") photoUrl: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun changePassword(
        @Url url: String,
        @Field("mode") mode: String = "changePassword",
        @Field("email") email: String,
        @Field("oldPassword") oldPassword: String,
        @Field("newPassword") newPassword: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun getUsers(
        @Url url: String,
        @Field("mode") mode: String = "getUsers"
    ): GetUsersResponse

    @POST
    @FormUrlEncoded
    suspend fun sendMessage(
        @Url url: String,
        @Field("mode") mode: String = "sendMessage",
        @Field("message") message: String,
        @Field("timestamp") timestamp: String,
        @Field("username") username: String,
        @Field("participants") participantsJson: String,
        @Field("replyTo") replyToJson: String? = null,
        @Field("isReaction") isReaction: String? = null
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun deleteMessage(
        @Url url: String,
        @Field("mode") mode: String = "deleteMessage",
        @Field("id") id: String,
        @Field("sender") sender: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun uploadPhoto(
        @Url url: String,
        @Field("mode") mode: String = "uploadPhoto",
        @Field("email") email: String,
        @Field("photoData") base64PhotoData: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun getPairStatus(
        @Url url: String,
        @Field("mode") mode: String = "getPairStatus",
        @Field("email") email: String
    ): PairStatusResponse

    @POST
    @FormUrlEncoded
    suspend fun sendPairRequest(
        @Url url: String,
        @Field("mode") mode: String = "sendPairRequest",
        @Field("fromEmail") fromEmail: String,
        @Field("toEmail") toEmail: String
    ): CommonResponse

    @GET
    suspend fun getAppsScriptVersions(
        @Url url: String,
        @Query("action") action: String = "list"
    ): AppsScriptVersionListResponse

    @POST
    @FormUrlEncoded
    suspend fun addAppsScriptVersion(
        @Url url: String,
        @Field("action") action: String = "add",
        @Field("version") version: String,
        @Field("title") title: String,
        @Field("link") link: String,
        @Field("changes") changes: String
    ): AppsScriptVersionActionResponse

    @POST
    @FormUrlEncoded
    suspend fun editAppsScriptVersion(
        @Url url: String,
        @Field("action") action: String = "edit",
        @Field("id") id: String,
        @Field("version") version: String,
        @Field("title") title: String,
        @Field("link") link: String,
        @Field("changes") changes: String
    ): AppsScriptVersionActionResponse

    @POST
    @FormUrlEncoded
    suspend fun deleteAppsScriptVersion(
        @Url url: String,
        @Field("action") action: String = "delete",
        @Field("id") id: String
    ): AppsScriptVersionActionResponse

    @POST
    @FormUrlEncoded
    suspend fun respondPairRequest(
        @Url url: String,
        @Field("mode") mode: String = "respondPairRequest",
        @Field("fromEmail") fromEmail: String,
        @Field("toEmail") toEmail: String,
        @Field("accept") accept: String // "true" or "false"
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun checkPairRequest(
        @Url url: String,
        @Field("mode") mode: String = "checkPairRequest",
        @Field("email") email: String
    ): CheckPairResponse

    @POST
    @FormUrlEncoded
    suspend fun getResetCodeForPartner(
        @Url url: String,
        @Field("mode") mode: String = "getResetCodeForPartner",
        @Field("partnerEmail") partnerEmail: String
    ): PartnerCodeResponse

    @POST
    @FormUrlEncoded
    suspend fun sendPasswordResetCode(
        @Url url: String,
        @Field("mode") mode: String = "sendPasswordResetCode",
        @Field("email") email: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun verifyResetCode(
        @Url url: String,
        @Field("mode") mode: String = "verifyResetCode",
        @Field("email") email: String,
        @Field("code") code: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun resetPassword(
        @Url url: String,
        @Field("mode") mode: String = "resetPassword",
        @Field("email") email: String,
        @Field("newPassword") newPassword: String
    ): CommonResponse

    @POST
    @FormUrlEncoded
    suspend fun removePair(
        @Url url: String,
        @Field("mode") mode: String = "removePair",
        @Field("email") email: String
    ): CommonResponse

    @GET
    suspend fun getPremiumCodes(@Url url: String): List<PremiumCode>
}

interface FirebaseDatabaseApi {

    @PUT("online/{userKey}.json")
    suspend fun updateOnlineStatus(
        @Path("userKey") userKey: String,
        @Body body: Map<String, Any>
    ): Any

    @GET("online/{userKey}.json")
    suspend fun getOnlineStatus(
        @Path("userKey") userKey: String
    ): Map<String, Any>?

    @GET("online.json")
    suspend fun getAllOnlineStatuses(): Map<String, Map<String, Any>>?

    @PUT("typing/{chatKey}/{userKey}.json")
    suspend fun updateTypingStatus(
        @Path("chatKey") chatKey: String,
        @Path("userKey") userKey: String,
        @Body body: Map<String, Any>?
    ): Any

    @GET("typing/{chatKey}.json")
    suspend fun getTypingStatus(
        @Path("chatKey") chatKey: String
    ): Map<String, Map<String, Any>>?

    @DELETE("typing/{chatKey}/{userKey}.json")
    suspend fun deleteTypingStatus(
        @Path("chatKey") chatKey: String,
        @Path("userKey") userKey: String
    ): Any

    @PUT("sessions/{userKey}/activeSession.json")
    suspend fun updateActiveSession(
        @Path("userKey") userKey: String,
        @Body body: Map<String, Any>
    ): Any

    @GET("sessions/{userKey}/activeSession.json")
    suspend fun getActiveSession(
        @Path("userKey") userKey: String
    ): Map<String, Any>?

    @PUT("sessions/{userKey}/history/{sessionId}.json")
    suspend fun updateSessionHistory(
        @Path("userKey") userKey: String,
        @Path("sessionId") sessionId: String,
        @Body body: Map<String, Any>
    ): Any

    @PATCH("sessions/{userKey}/history/{sessionId}.json")
    suspend fun updateSessionLastActive(
        @Path("userKey") userKey: String,
        @Path("sessionId") sessionId: String,
        @Body body: Map<String, Any>
    ): Any

    @GET("sessions/{userKey}/history.json")
    suspend fun getSessionHistory(
        @Path("userKey") userKey: String
    ): Map<String, Map<String, Any>>?

    @DELETE("sessions/{userKey}/history/{sessionId}.json")
    suspend fun deleteSessionHistory(
        @Path("userKey") userKey: String,
        @Path("sessionId") sessionId: String
    ): Any

    @PUT("calls/{roomId}.json")
    suspend fun createCallRoom(
        @Path("roomId") roomId: String,
        @Body body: Map<String, Any>
    ): Any

    @PATCH("calls/{roomId}.json")
    suspend fun patchCallRoom(
        @Path("roomId") roomId: String,
        @Body body: Map<String, Any>
    ): Any

    @DELETE("calls/{roomId}.json")
    suspend fun deleteCallRoom(
        @Path("roomId") roomId: String
    ): Any

    @GET("calls/{roomId}.json")
    suspend fun getCallRoom(
        @Path("roomId") roomId: String
    ): Map<String, Any>?

    @GET("calls.json")
    suspend fun getAllCalls(): Map<String, Map<String, Any>>?

    @PUT("calls/{roomId}/candidates/{role}/{timestamp}.json")
    suspend fun addCallCandidate(
        @Path("roomId") roomId: String,
        @Path("role") role: String, // "caller" or "callee"
        @Path("timestamp") timestamp: String,
        @Body body: Map<String, Any>
    ): Any
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val echoChatApi: EchoChatApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://script.google.com/") // Base, but overridden using @Url
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EchoChatApi::class.java)
    }
}

// Simple wrapper API helper for Supabase Database REST calls
interface SupabaseRestService {
    suspend fun setValue(path: String, body: Any): Any
    suspend fun getValue(path: String): Any?
    suspend fun deleteValue(path: String): Any
    suspend fun patchValue(path: String, body: Any): Any
}

class SupabaseRestServiceImpl(
    private val client: OkHttpClient,
    private val moshi: Moshi
) : SupabaseRestService {

    private val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJ2ZmJseXRteGx2dHdndXJuenRwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIzOTE1NTQsImV4cCI6MjA5Nzk2NzU1NH0.zypeY87lYwhGSjzfcyzV16N4VuvKwUaQJxWIzGzU-2s"
    private val baseUrl = "https://bvfblytmxlvtwgurnztp.supabase.co/rest/v1/kv_store"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    override suspend fun setValue(path: String, body: Any): Any {
        val sanitizedPath = path.removeSuffix(".json")
        // Serialize body
        val bodyAdapter = moshi.adapter(Any::class.java)
        val bodyJson = bodyAdapter.toJson(body)

        // Create envelope
        val envelope = mapOf("id" to sanitizedPath, "data" to body)
        val envelopeAdapter = moshi.adapter(Map::class.java)
        val envelopeJson = envelopeAdapter.toJson(envelope)

        val requestBody = envelopeJson.toRequestBody(jsonMediaType)

        val request = okhttp3.Request.Builder()
            .url(baseUrl)
            .post(requestBody)
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .addHeader("Content-Type", "application/json")
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    throw java.io.IOException("Supabase setValue error: ${response.code} $err")
                }
                true
            }
        }
    }

    override suspend fun getValue(path: String): Any? {
        val sanitizedPath = path.removeSuffix(".json")
        // We fetch exact or prefix matches
        val url = baseUrl.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("or", "(id.eq.$sanitizedPath,id.like.$sanitizedPath/%)")
            .addQueryParameter("select", "id,data")
            .build()

        val request = okhttp3.Request.Builder()
            .url(url)
            .get()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    if (response.code == 404) return@withContext null
                    throw java.io.IOException("Supabase getValue error: ${response.code} $err")
                }
                val bodyString = response.body?.string() ?: return@withContext null
                
                // Parse as list of maps
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, Map::class.java)
                val adapter = moshi.adapter<List<Map<String, Any>>>(listType)
                val rows = try {
                    adapter.fromJson(bodyString) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                if (rows.isEmpty()) {
                    return@withContext null
                }

                // If we have exact match only and no other rows
                val exactRow = rows.find { it["id"] == sanitizedPath }
                if (exactRow != null && rows.size == 1) {
                    return@withContext exactRow["data"]
                }

                // Compile prefix match as a Map
                val resultMap = mutableMapOf<String, Any>()
                for (row in rows) {
                    val rowId = row["id"] as? String ?: continue
                    val rowData = row["data"] ?: continue
                    if (rowId == sanitizedPath) {
                        if (rowData is Map<*, *>) {
                            rowData.forEach { (k, v) ->
                                if (k != null && v != null) {
                                    resultMap[k.toString()] = v
                                }
                            }
                        }
                    } else {
                        val remainder = rowId.substring(sanitizedPath.length)
                        val subkey = if (remainder.startsWith("/")) remainder.substring(1) else remainder
                        resultMap[subkey] = rowData
                    }
                }
                
                if (resultMap.isEmpty() && exactRow != null) {
                    return@withContext exactRow["data"]
                }
                
                resultMap
            }
        }
    }

    override suspend fun deleteValue(path: String): Any {
        val sanitizedPath = path.removeSuffix(".json")
        val url = baseUrl.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("or", "(id.eq.$sanitizedPath,id.like.$sanitizedPath/%)")
            .build()

        val request = okhttp3.Request.Builder()
            .url(url)
            .delete()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $anonKey")
            .build()

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    throw java.io.IOException("Supabase deleteValue error: ${response.code} $err")
                }
                true
            }
        }
    }

    override suspend fun patchValue(path: String, body: Any): Any {
        val sanitizedPath = path.removeSuffix(".json")
        val existing = getValue(sanitizedPath) as? Map<String, Any>
        val merged = if (existing != null && body is Map<*, *>) {
            val updated = existing.toMutableMap()
            body.forEach { (k, v) ->
                if (k != null) {
                    if (v != null) {
                        updated[k.toString()] = v
                    } else {
                        updated.remove(k.toString())
                    }
                }
            }
            updated
        } else {
            body
        }
        return setValue(sanitizedPath, merged)
    }
}

object SupabaseRestClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: SupabaseRestService by lazy {
        SupabaseRestServiceImpl(okHttpClient, moshi)
    }
}

object FirebaseRestClient {
    val service: SupabaseRestService get() = SupabaseRestClient.service
}
