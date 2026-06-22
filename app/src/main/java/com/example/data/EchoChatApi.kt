package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

// Simple wrapper API helper for Firebase Database REST calls
interface FirebaseRestService {
    @retrofit2.http.PUT("{path}.json")
    suspend fun setValue(@retrofit2.http.Path(value = "path", encoded = true) path: String, @retrofit2.http.Body body: Any): Any

    @retrofit2.http.GET("{path}.json")
    suspend fun getValue(@retrofit2.http.Path(value = "path", encoded = true) path: String): Any?

    @retrofit2.http.DELETE("{path}.json")
    suspend fun deleteValue(@retrofit2.http.Path(value = "path", encoded = true) path: String): Any

    @retrofit2.http.PATCH("{path}.json")
    suspend fun patchValue(@retrofit2.http.Path(value = "path", encoded = true) path: String, @retrofit2.http.Body body: Any): Any
}

object FirebaseRestClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    val service: FirebaseRestService by lazy {
        Retrofit.Builder()
            .baseUrl("https://rafid-chat-default-rtdb.firebaseio.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FirebaseRestService::class.java)
    }
}
