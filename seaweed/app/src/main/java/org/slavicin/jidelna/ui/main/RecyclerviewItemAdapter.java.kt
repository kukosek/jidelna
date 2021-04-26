package org.slavicin.jidelna.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import org.slavicin.jidelna.Action
import org.slavicin.jidelna.DinnerRequestParams
import org.slavicin.jidelna.R
import org.slavicin.jidelna.RestApi
import org.slavicin.jidelna.data.CantryMenu
import org.slavicin.jidelna.data.Dinner
import org.slavicin.jidelna.data.getString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.*

object Status {
    const val ORDERED = "ordered"
    const val ORDERING = "ordering"
    const val CANCELLING_ORDER = "cancelling order"
    const val AVAILABLE = "available"
    const val AUTOORDER = "autoordered"
}

class DinnerItemAdapter internal constructor(
    mItemList: List<Dinner>,
    private val date: String,
    private val service: RestApi,
    val rootLayout: SwipeRefreshLayout,
    val loginIntent: Intent,
    val dataSetChanged: () -> Unit,
    val context: Context
) :
    RecyclerView.Adapter<DinnerItemAdapter.MyViewHolder>() {
    private val itemsList: List<Dinner> = mItemList
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.dinner_view, parent, false)
        return MyViewHolder(view)
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item: Dinner = itemsList[position]
        holder.name.text = item.name

        holder.checkbox.isChecked = when (item.status) {
            Status.AVAILABLE -> false
            Status.ORDERED -> true
            Status.ORDERING -> true
            Status.CANCELLING_ORDER -> false
            Status.AUTOORDER -> true
            else -> false
        }



        holder.checkbox.setOnClickListener {
            holder.checkbox.visibility = View.GONE
            holder.progressBar.visibility = View.VISIBLE
            val action : String = if(holder.checkbox.isChecked){
                Action.ORDER
            }else{
                Action.CANCEL_ORDER
            }
            val callAsync : Call<Void> = service.requestDinner(
                DinnerRequestParams(
                    action,
                    date,
                    item.menuNumber
                )
            )
            callAsync.enqueue(object : Callback<Void?> {
                override fun onResponse(
                    call: Call<Void?>,
                    response: Response<Void?>
                ) {
                    holder.progressBar.visibility = View.GONE
                    holder.checkbox.visibility = View.VISIBLE
                    if (response.isSuccessful) {
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
                    call: Call<Void?>,
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
        var name: TextView = itemView.findViewById(R.id.dinnerName)
        var checkbox: CheckBox = itemView.findViewById(R.id.checkBox)
        var progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }
}


class MenuItemAdapter internal constructor(
    mItemList: List<CantryMenu>,
    private val service: RestApi,
    private val rootLayout: SwipeRefreshLayout,
    private val loginIntent: Intent
) :
    RecyclerView.Adapter<MenuItemAdapter.MyViewHolder>() {
    private val itemsList: List<CantryMenu> = mItemList
    private lateinit var context : Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        context = parent.context
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.menu_view, parent, false)
        return MyViewHolder(view)
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item: CantryMenu = itemsList[position]

        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.date);

        holder.name.text = SimpleDateFormat("E d. M.",
            Locale.getDefault()).format(date!!)
        holder.recyclerView.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(context)
        holder.recyclerView.layoutManager = layoutManager

        val dinnerItemAdapter = DinnerItemAdapter(
            item.menus,
            item.date,
            service,
            rootLayout,
            loginIntent,
            ::notifyDataSetChanged,
            context
        )
        holder.recyclerView.adapter = dinnerItemAdapter
    }

    override fun getItemCount(): Int {
        return itemsList.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var name: TextView = itemView.findViewById(R.id.menuName)
        var recyclerView : RecyclerView = itemView.findViewById(R.id.dinners)
    }

}