package org.slavicin.jidelna.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.slavicin.jidelna.consts.COOKIE_SET_KEY
import org.slavicin.jidelna.data.DinnerRequestCallback
import org.slavicin.jidelna.data.ReviewList
import org.slavicin.jidelna.data.UserSetting
import org.slavicin.jidelna.data.WeekMenu
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Headers
import java.io.IOException
import java.util.concurrent.TimeUnit


data class UserCredentials(
    @SerializedName("username") val userName: String?,
    @SerializedName("password") val password: String?
)
object Action {
    val ORDER = "order"
    val CANCEL_ORDER = "cancel"
}
data class DinnerRequestParams(
    @SerializedName("action") val action: String,
    @SerializedName("date") val date: String,
    @SerializedName("menuNumber") val menuNumber: Int
)

data class ReviewParams(
    @SerializedName("rating") val rating: Float,
    @SerializedName("message") val message: String
)

data class PostReviewParams(
    @SerializedName("review") val review: ReviewParams
)

data class ReviewScore(
    @SerializedName("userScore") val userScore: Double
)

data class PostReviewScoreParams(
    @SerializedName("score") val reviewScore: ReviewScore
)

public interface RestApi {
    @Headers("Content-Type: application/json")
    @POST("login")
    fun login(@Body userData: UserCredentials): Call<Void>

    @GET("logout")
    fun logout(@Query("delete") delete: Boolean): Call<Void>

    @GET("menu")
    fun getMenus(): Call<WeekMenu>

    @GET("menu")
    fun getMenu(@Query("date") date: String): Call<WeekMenu> //accepts date in iso string

    @GET("reviews")
    fun getReviews(@Query("dinnerid") dinnerids: ArrayList<Int>): Call<ReviewList> //accepts date in iso string

    @GET("reviews")
    fun getUserReviews(@Query("dinnerid") dinnerids: ArrayList<Int>, @Query("me") me: Boolean): Call<ReviewList> //accepts date in iso string

    @POST("reviews")
    fun postReview(@Query("dinnerid") dinnerids: ArrayList<Int>, @Body params: PostReviewParams): Call<Void>

    @POST("reviews")
    fun postReviewScore(@Query("reviewid") reviewid:Int, @Body params: PostReviewScoreParams): Call<Void>

    @POST("menu")
    fun requestDinner(@Body params: DinnerRequestParams): Call<DinnerRequestCallback>

    @GET("settings")
    fun getSettings(): Call<UserSetting>

    @POST("settings")
    fun updateSettings(@Body userSetting: UserSetting): Call<Void>
}

class ServiceBuilder constructor(){
    fun build(baseUrl: String, preferences: SharedPreferences, cachedClient: Boolean, context: Context) : RestApi {
        val cacheSize = (5 * 1024 * 1024).toLong()
        val myCache = Cache(context.cacheDir, cacheSize)

        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder()
            .addInterceptor(AddCookiesInterceptor(preferences))
            .addInterceptor(ReceivedCookiesInterceptor(preferences)
            ).addInterceptor(logging)
            .cache(myCache)
            .addInterceptor { chain ->
                var request = chain.request()
                request = if (!cachedClient)
                    request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                else
                    request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build()
                chain.proceed(request)
            }
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(
                RxJava2CallAdapterFactory.create()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(RestApi::class.java)
    }

}

class ReceivedCookiesInterceptor(private val preferences: SharedPreferences) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse: Response = chain.proceed(chain.request())
        if (!originalResponse.headers("Set-Cookie").isEmpty()) {
            val cookies =  preferences.getStringSet(
                COOKIE_SET_KEY,
                HashSet()
            ) as HashSet<String>?
            for (header in originalResponse.headers("Set-Cookie")) {
                cookies!!.add(header)
            }
            preferences.edit{
                this.remove(COOKIE_SET_KEY)
                this.apply()
                this.putStringSet(COOKIE_SET_KEY, cookies)
            }
        }
        return originalResponse
    }

}

class AddCookiesInterceptor(private val preferences: SharedPreferences) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val preferences =
            preferences.getStringSet(
                COOKIE_SET_KEY,
                HashSet()
            ) as HashSet<String>?
        val original: Request = chain.request()
        for (cookie in preferences!!) {
            builder.addHeader("Cookie", cookie)
        }
        return chain.proceed(builder.build())
    }


}