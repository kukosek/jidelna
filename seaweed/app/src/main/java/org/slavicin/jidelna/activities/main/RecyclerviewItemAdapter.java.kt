package org.slavicin.jidelna.activities.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.formats.MediaView
import com.google.android.gms.ads.formats.UnifiedNativeAdView
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.R
import org.slavicin.jidelna.activities.reviews.ReviewsActivity
import org.slavicin.jidelna.data.Dinner
import org.slavicin.jidelna.data.DinnerRequestCallback
import org.slavicin.jidelna.data.getString
import org.slavicin.jidelna.network.Action
import org.slavicin.jidelna.network.DinnerRequestParams
import org.slavicin.jidelna.network.RestApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


object Status {
    const val ORDERED = "ordered"
    const val ORDERED_CLOSED = "ordered closed"
    const val ORDERING = "ordering"
    const val CANCELLING_ORDER = "cancelling order"
    const val AVAILABLE = "available"
    const val AUTOORDER = "autoordered"
    const val UNAVAILABLE = "unavailable"
}

class DinnerItemAdapter internal constructor(
    mItemList: List<Dinner>,
    private val date: String,
    private val service: RestApi,
    val rootLayout: SwipeRefreshLayout,
    val loginIntent: Intent,
    val dataSetChanged: () -> Unit,
    val setCreditLeft: (creditLeft: Int) -> Unit,
    val context: Context
) :
    RecyclerView.Adapter<DinnerItemAdapter.MyViewHolder>() {
    private val itemsList: List<Dinner> = mItemList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.dinner_view, parent, false)

        return MyViewHolder(view)
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item: Dinner = itemsList[position]
        holder.name.text = item.name

        holder.checkbox.isChecked = when (item.status) {
            Status.AVAILABLE -> false
            Status.ORDERED -> true
            Status.ORDERED_CLOSED -> true
            Status.ORDERING -> true
            Status.CANCELLING_ORDER -> false
            Status.AUTOORDER -> true
            else -> false
        }
        holder.checkbox.isEnabled = (item.status != Status.UNAVAILABLE) && (item.status != Status.ORDERED_CLOSED)


        holder.checkbox.setOnClickListener {
            holder.checkbox.visibility = View.GONE
            holder.progressBar.visibility = View.VISIBLE
            val action : String = if(holder.checkbox.isChecked){
                Action.ORDER
            }else{
                Action.CANCEL_ORDER
            }
            val callAsync : Call<DinnerRequestCallback> = service.requestDinner(
                DinnerRequestParams(
                    action,
                    date,
                    item.menuNumber
                )
            )
            callAsync.enqueue(object : Callback<DinnerRequestCallback?> {
                override fun onResponse(
                    call: Call<DinnerRequestCallback?>,
                    response: Response<DinnerRequestCallback?>
                ) {
                    holder.progressBar.visibility = View.GONE
                    holder.checkbox.visibility = View.VISIBLE
                    if (response.isSuccessful) {
                        val creditLeft = response.body()!!.creditLeft
                        setCreditLeft(creditLeft)
                        if (action == Action.ORDER) {
                            for (mItem in itemsList) {
                                mItem.status = Status.AVAILABLE
                            }
                            item.status = Status.ORDERED
                        } else {
                            item.status = Status.AVAILABLE
                        }
                        dataSetChanged()
                    } else {
                        holder.checkbox.isChecked = action != Action.ORDER
                        if (response.code() == 401) {
                            startActivity(context, loginIntent, Bundle())
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
                    call: Call<DinnerRequestCallback?>,
                    t: Throwable
                ) {
                    holder.progressBar.visibility = View.GONE
                    holder.checkbox.visibility = View.VISIBLE
                    holder.checkbox.isChecked = action != Action.ORDER
                    val errorMessage = getString(context, "network_error") + t.localizedMessage
                    Snackbar.make(
                        rootLayout,
                        errorMessage,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.reviewInfo)
        var checkbox: CheckBox = itemView.findViewById(R.id.checkBox)
        var progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }
}


class MenuItemAdapter internal constructor(
    mItemList: List<MenuRecyclerviewItem>,
    private val service: RestApi,
    private val rootLayout: SwipeRefreshLayout,
    private val loginIntent: Intent,
    private val setCreditLeft: (creditLeft: Int) -> Unit,
    val context: Context
) :
    RecyclerView.Adapter<MenuItemAdapter.MyViewHolder>() {
    private val itemsList: List<MenuRecyclerviewItem> = mItemList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater: LayoutInflater= context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View =
            layoutInflater.inflate(R.layout.menu_view, parent, false)
        return MyViewHolder(view)
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item: MenuRecyclerviewItem = itemsList[position]

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.parse(item.cantryMenu.date);
        holder.name.text = SimpleDateFormat(
            "E d. M.",
            Locale.getDefault()
        ).format(date!!)
        val today = dateFormat.format(Calendar.getInstance().time);

        val isToday = today == item.cantryMenu.date

        var numOfReviews = 0
        val dinnerids = arrayListOf<Int>()

        for (dinner in item.cantryMenu.menus) {
            numOfReviews += dinner.numOfReviews
            dinnerids.add( dinner.dinnerid)
        }

        if (numOfReviews == 0 && !isToday) {
            holder.seeCommentsButton.visibility = GONE
        }else {
            holder.seeCommentsButton.visibility = VISIBLE
        }
        holder.seeCommentsButton.text = "${context.getString( R.string.see_all)} ($numOfReviews)"

        holder.seeCommentsButton.setOnClickListener {
            val intent = Intent(context, ReviewsActivity::class.java)
            intent.putExtra("dinnerids", dinnerids)

            var canOrder = false
            for (menu in item.cantryMenu.menus) {
                if (menu.status == Status.ORDERED || menu.status == Status.ORDERED_CLOSED) {
                    canOrder = true
                }
            }
            intent.putExtra("canOrder", canOrder)

            startActivity(context, intent, null)

        }

        holder.recyclerView.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(context)
        holder.recyclerView.layoutManager = layoutManager

        val dinnerItemAdapter = DinnerItemAdapter(
            item.cantryMenu.menus,
            item.cantryMenu.date,
            service,
            rootLayout,
            loginIntent,
            ::notifyDataSetChanged,
            setCreditLeft,
            context
        )
        holder.recyclerView.adapter = dinnerItemAdapter

        //ad related thingy
        if (item.ad != null) {
            holder.adView.visibility = View.VISIBLE
            // The MediaView will display a video asset if one is present in the ad, and the
            // first image asset otherwise.
            // The MediaView will display a video asset if one is present in the ad, and the
            // first image asset otherwise.
            with(holder){
                adView.mediaView = adView.findViewById(R.id.ad_media) as MediaView

                // Register the view used for each individual asset.

                // Register the view used for each individual asset.
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_icon)
                adView.priceView = adView.findViewById(R.id.ad_price)
                adView.starRatingView = adView.findViewById(R.id.ad_stars)
                adView.storeView = adView.findViewById(R.id.ad_store)
                adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
            }
            populateNativeAdView(item.ad!!, holder.adView)
        }else {
            holder.adView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.menuName)
        var recyclerView : RecyclerView = itemView.findViewById(R.id.dinners)
        val adView: UnifiedNativeAdView = itemView.findViewById(R.id.ad_view)
        val cardView: CardView = itemView.findViewById(R.id.card_view)
        val seeCommentsButton: Button = itemView.findViewById(R.id.btnSeeAll)
    }

}