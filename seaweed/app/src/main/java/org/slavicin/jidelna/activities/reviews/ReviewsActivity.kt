package org.slavicin.jidelna.activities.reviews

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.login.LoginActivity
import org.slavicin.jidelna.activities.logout
import org.slavicin.jidelna.activities.reviewwrite.ReviewWriteActivity
import org.slavicin.jidelna.consts.*
import org.slavicin.jidelna.data.Review
import org.slavicin.jidelna.data.ReviewList
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.ServiceBuilder
import org.slavicin.jidelna.utlis.setAppTheme
import org.slavicin.jidelna.utlis.setSystemNavBarColor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReviewsActivity : AppCompatActivity() {
    private lateinit var appPreferences: SharedPreferences
    lateinit var cookiePreferences: SharedPreferences
    lateinit var loginIntent: Intent
    lateinit var service : RestApi
    lateinit var rootLayout : SwipeRefreshLayout
    var reviews = mutableListOf<Review>()
    lateinit var reviewsRecyclerView: RecyclerView
    lateinit var noReviewsLayout: ConstraintLayout
    private lateinit var reviewsItemAdapter : ReviewItemAdapter

    var dinnerids = arrayListOf<Int>()

    private fun reloadReviews() {
        rootLayout.isRefreshing = true;
        val callAsync  = service.getReviews(dinnerids)
        callAsync.enqueue(object : Callback<ReviewList?> {
            override fun onResponse(
                call: Call<ReviewList?>,
                response: Response<ReviewList?>
            ) {
                if (response.isSuccessful) {
                    val reviewsNew = response.body()!!.reviews
                    reviews.clear()
                    for (review in reviewsNew) {
                        reviews.add(review)
                    }
                    if (reviews.size == 0) {
                        noReviewsLayout.visibility = VISIBLE
                    } else {
                        noReviewsLayout.visibility = INVISIBLE
                    }

                    reviewsItemAdapter.notifyDataSetChanged();
                } else {
                    if (response.code() == 401) {
                        logout(cookiePreferences, this@ReviewsActivity)
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
                rootLayout.isRefreshing = false
            }

            override fun onFailure(
                call: Call<ReviewList?>,
                t: Throwable
            ) {
                rootLayout.isRefreshing = false
                val errorMessage = resources.getString(R.string.network_error) + t.localizedMessage
                Snackbar.make(
                    rootLayout,
                    errorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })

    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("dinnerids", dinnerids)
    }

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

        setAppTheme(this)
        appPreferences = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
        cookiePreferences = getSharedPreferences(COOKIE_PREFS_NAME, Context.MODE_PRIVATE)
        loginIntent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        val cookies =
            cookiePreferences.getStringSet(
                COOKIE_SET_KEY,
                HashSet()
            ) as HashSet<String>?

        setContentView(R.layout.activity_reviews)
        setSystemNavBarColor(this, window)

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            val intent = Intent(this, ReviewWriteActivity::class.java)
            intent.putExtra("dinnerids", dinnerids)
            ContextCompat.startActivity(this, intent, null)
        }

        service = ServiceBuilder().build(
            appPreferences.getString(
                APP_BASE_URL_KEY,
                APP_BASE_URL_DEFAULT
            )!!, cookiePreferences
        )

        noReviewsLayout = findViewById(R.id.no_reviews_layout)
        rootLayout = findViewById<SwipeRefreshLayout>(R.id.main_layout)
        rootLayout.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
            reloadReviews()
        })

        reviewsRecyclerView = findViewById(R.id.reviews)
        reviewsItemAdapter = ReviewItemAdapter(reviews, service, rootLayout, loginIntent,
            this
        )
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(
            this@ReviewsActivity
        )

        reviewsRecyclerView.layoutManager = layoutManager;
        reviewsRecyclerView.adapter = reviewsItemAdapter;
        setAppTheme(this)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        reloadReviews()
    }
}