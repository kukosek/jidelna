package org.slavicin.jidelna

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.slavicin.jidelna.data.CantryMenu
import org.slavicin.jidelna.data.UserSetting
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import java.io.IOException


data class UserCredentials (
    @SerializedName("username") val userName: String?,
    @SerializedName("password") val password: String?
)
object Action {
    val ORDER = "order"
    val CANCEL_ORDER = "cancel"
}
data class DinnerRequestParams (
    @SerializedName("action") val action: String,
    @SerializedName("date") val date: String,
    @SerializedName("menuNumber") val menuNumber: Int
)

public interface RestApi {
    @Headers("Content-Type: application/json")
    @POST("login")
    fun login(@Body userData: UserCredentials): Call<Void>

    @GET("menu")
    fun getMenus(): Call<List<CantryMenu>>

    @POST("menu")
    fun requestDinner(@Body params: DinnerRequestParams): Call<Void>

    @GET("settings")
    fun getSettings(): Call<UserSetting>

    @POST("settings")
    fun updateSettings(@Body userSetting: UserSetting): Call<Void>
}

class ServiceBuilder constructor(){
    fun build(baseUrl: String, preferences: SharedPreferences) : RestApi {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val client = OkHttpClient.Builder().addInterceptor(AddCookiesInterceptor(preferences)).addInterceptor(ReceivedCookiesInterceptor(preferences)).addInterceptor(logging).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addCallAdapterFactory(
                RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(RestApi::class.java)
    }

}

class ReceivedCookiesInterceptor(preferences: SharedPreferences) : Interceptor {
    private val preferences: SharedPreferences
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse: Response = chain.proceed(chain.request())
        if (!originalResponse.headers("Set-Cookie").isEmpty()) {
            val cookies =  preferences.getStringSet(
                    "PREF_COOKIES",
                    HashSet()
                ) as HashSet<String>?
            for (header in originalResponse.headers("Set-Cookie")) {
                cookies!!.add(header)
            }
            preferences.edit{
                this.remove("PREF_COOKIES")
                this.apply()
                this.putStringSet("PREF_COOKIES", cookies)
            }
        }
        return originalResponse
    }

    init {
        this.preferences = preferences
    }
}

class AddCookiesInterceptor(preferences: SharedPreferences) : Interceptor {
    private val preferences: SharedPreferences = preferences

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder: Request.Builder = chain.request().newBuilder()
        val preferences =
            preferences.getStringSet(
                PREF_COOKIES,
                HashSet()
            ) as HashSet<String>?
        val original: Request = chain.request()
        for (cookie in preferences!!) {
            builder.addHeader("Cookie", cookie)
        }
        return chain.proceed(builder.build())
    }

    companion object {

        const val PREF_COOKIES = "PREF_COOKIES"
    }

}