package org.slavicin.jidelna.activities.reviews

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.jackandphantom.androidlikebutton.AndroidLikeButton
import com.jackandphantom.androidlikebutton.AndroidLikeButton.OnLikeEventListener
import org.slavicin.jidelna.R
import org.slavicin.jidelna.data.Review
import org.slavicin.jidelna.data.getString
import org.slavicin.jidelna.network.PostReviewScoreParams
import org.slavicin.jidelna.network.RestApi
import org.slavicin.jidelna.network.ReviewScore
import org.slavicin.jidelna.utlis.agoString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ReviewItemAdapter internal constructor(
    mItemList: List<Review>,
    private val service: RestApi,
    val rootLayout: SwipeRefreshLayout,
    val loginIntent: Intent,
    val context: Context
) :
    RecyclerView.Adapter<ReviewItemAdapter.MyViewHolder>() {
    private val itemsList: List<Review> = mItemList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater: LayoutInflater= context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View =
            layoutInflater.inflate(R.layout.review_view, parent, false)
        return MyViewHolder(view)
    }

    fun onUserScoreChange(androidLikeButton: AndroidLikeButton, item: Review, like: Boolean, holder: MyViewHolder) {

        val callAsync : Call<Void> = service.postReviewScore(item.id, PostReviewScoreParams(
            ReviewScore(when(like) {
                true -> 1.0
                false -> 0.0
            })
        ))
        callAsync.enqueue(object : Callback<Void?> {
            override fun onResponse(
                call: Call<Void?>,
                response: Response<Void?>
            ) {
                if (response.isSuccessful) {
                    notifyDataSetChanged()
                } else {
                    if (response.code() == 401) {
                        ContextCompat.startActivity(context, loginIntent, Bundle())
                    } else {
                        val errorMessage: String = when {
                            response.code() == 502 -> {
                                getString(context, "gateway_timeout")
                            }
                            else -> {
                                getString(context, "request_error") + response.errorBody()
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
                val errorMessage = getString(context, "network_error") + t.localizedMessage
                Snackbar.make(
                    rootLayout,
                    errorMessage,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item: Review = itemsList[position]
        holder.info.text = "${item.user} hodnotí ${item.dinnerName}"
        holder.message.text = item.message
        holder.likeButton.clipToOutline = true
        holder.score.text = item.score.toInt().toString()

        if (item.userScore == 1.0) {
            holder.likeButton.setCurrentlyLiked(true)
        }


        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        val datePosted: Date = format.parse(item.datePosted)



        holder.date.text = agoString(context, datePosted)

        holder.likeButton.setOnLikeEventListener(object : OnLikeEventListener {
            override fun onLikeClicked(androidLikeButton: AndroidLikeButton) {
                onUserScoreChange(androidLikeButton, item, true, holder)
            }
            override fun onUnlikeClicked(androidLikeButton: AndroidLikeButton) {
                onUserScoreChange(androidLikeButton, item, false, holder)
            }
        })

    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var info: TextView = itemView.findViewById(R.id.reviewInfo)
        var message: TextView = itemView.findViewById(R.id.reviewMessage)
        var date: TextView = itemView.findViewById(R.id.reviewDate)
        var likeButton: AndroidLikeButton = itemView.findViewById(R.id.androidLikeButton)
        var score: TextView = itemView.findViewById(R.id.reviewScore)
    }
}
