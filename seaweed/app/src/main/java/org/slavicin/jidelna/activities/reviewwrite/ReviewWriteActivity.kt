package org.slavicin.jidelna.activities.reviewwrite

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RatingBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_review_write.*
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.login.LoginActivity
import org.slavicin.jidelna.activities.logout
import org.slavicin.jidelna.consts.APP_BASE_URL_DEFAULT
import org.slavicin.jidelna.consts.APP_BASE_URL_KEY
import org.slavicin.jidelna.consts.APP_PREFS_NAME
import org.slavicin.jidelna.consts.COOKIE_PREFS_NAME
import org.slavicin.jidelna.data.ReviewList
import org.slavicin.jidelna.network.*
import org.slavicin.jidelna.utlis.setAppTheme
import org.slavicin.jidelna.utlis.setSystemNavBarColor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReviewWriteActivity : AppCompatActivity() {
    var dinnerids = arrayListOf<Int>()
    lateinit var progressBar: ProgressBar
    lateinit var service: RestApi
    lateinit var rootLayout: CoordinatorLayout
    lateinit var loginIntent: Intent

    lateinit var starRatingBar: RatingBar
    lateinit var messageEditText: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (savedInstanceState == null) {
            val extras = intent.extras
            if (extras != null) {
                dinnerids = extras.getIntegerArrayList("dinnerids") as ArrayList<Int>
            }
        } else {
            dinnerids = savedInstanceState.getSerializable("dinnerids") as ArrayList<Int>
        }

        setContentView(R.layout.activity_review_write)
        setAppTheme(this)
        setSystemNavBarColor(this, window)


        val appPreferences = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        val cookiePreferences = getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)

        service = ServiceBuilder().build(
            appPreferences.getString(
                APP_BASE_URL_KEY,
                APP_BASE_URL_DEFAULT
            )!!, cookiePreferences
        )

        loginIntent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        progressBar = findViewById(R.id.sendProgressBar)
        progressBar.isIndeterminate = true
        starRatingBar = findViewById(R.id.ratingBar)
        messageEditText = findViewById(R.id.messageEditText)
        rootLayout = findViewById(R.id.coordinatorLayoutReviewWrite)
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            progressBar.visibility = VISIBLE
            fab.isEnabled = false
            val previousFabTint = fab.backgroundTintList
            fab.backgroundTintList = ContextCompat.getColorStateList(applicationContext, R.color.textColorTertiary);


            val callAsync : Call<Void> = service.postReview(dinnerids, PostReviewParams(ReviewParams(
                starRatingBar.rating, messageEditText.text.toString()
            ))
            )
            callAsync.enqueue(object : Callback<Void?> {
                override fun onResponse(
                    call: Call<Void?>,
                    response: Response<Void?>
                ) {
                    if (response.isSuccessful) {
                        finish()
                    } else {
                        fab.isEnabled = true
                        progressBar.visibility = INVISIBLE
                        fab.backgroundTintList = previousFabTint
                        if (response.code() == 401) {
                            ContextCompat.startActivity(this@ReviewWriteActivity, loginIntent, Bundle())
                        } else {
                            val errorMessage: String = when {
                                response.code() == 502 -> {
                                    org.slavicin.jidelna.data.getString(this@ReviewWriteActivity, "gateway_timeout")
                                }
                                response.code() == 400 -> {
                                    org.slavicin.jidelna.data.getString(this@ReviewWriteActivity, "must_order_to_write_review")
                                }
                                else -> {
                                    org.slavicin.jidelna.data.getString(this@ReviewWriteActivity, "request_error") + response.errorBody()
                                }
                            }
                            Snackbar.make(
                                rootLayout,
                                errorMessage,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

                override fun onFailure(
                    call: Call<Void?>,
                    t: Throwable
                ) {
                    fab.isEnabled = true
                    progressBar.visibility = INVISIBLE
                    fab.backgroundTintList = previousFabTint
                    val errorMessage = org.slavicin.jidelna.data.getString(
                        this@ReviewWriteActivity,
                        "network_error"
                    ) + t.localizedMessage
                    Snackbar.make(
                        rootLayout,
                        errorMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            })
        }
        loadReview()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("dinnerids", dinnerids)
    }

    private fun loadReview() {
        starRatingBar.isEnabled = false
        messageEditText.isEnabled = false
        progressBar.visibility = VISIBLE;
        val callAsync = service.getUserReviews(dinnerids, true)
        callAsync.enqueue(object : Callback<ReviewList?> {
            override fun onResponse(
                call: Call<ReviewList?>,
                response: Response<ReviewList?>
            ) {
                if (response.isSuccessful) {
                    val reviewsNew = response.body()!!.reviews
                    if (reviewsNew.isNotEmpty()) {
                        val review = reviewsNew[0]
                        messageEditText.setText(review.message)
                        starRatingBar.rating = review.rating.toFloat()
                    }
                } else {
                    if (response.code() == 401) {
                        ContextCompat.startActivity(this@ReviewWriteActivity, loginIntent, Bundle())
                    } else {
                        val errorMessage = when (response.code()) {
                            500 -> resources.getString(R.string.server_error);
                            502 -> resources.getString(R.string.gateway_timeout);
                            else -> resources.getString(R.string.request_error) + response.errorBody()
                        }
                        Snackbar.make(
                            rootLayout,
                            errorMessage,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }

                }
                progressBar.visibility = INVISIBLE
                messageEditText.isEnabled = true
                starRatingBar.isEnabled = true
            }

            override fun onFailure(
                call: Call<ReviewList?>,
                t: Throwable
            ) {
                progressBar.visibility = INVISIBLE
                starRatingBar.isEnabled = true
                messageEditText.isEnabled = true
                val errorMessage = resources.getString(R.string.network_error) + t.localizedMessage
                Snackbar.make(
                    rootLayout,
                    errorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })
    }
}