package com.udacity.project4.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter


object BindingAdapters {

    /**
     * Use binding adapter to set the recycler view data using livedata object
     */
    @Suppress("UNCHECKED_CAST")
    @BindingAdapter("android:liveData")
    @JvmStatic
    fun <T> setRecyclerViewData(recyclerView: RecyclerView, items: LiveData<List<T>>?) {
        items?.value?.let { itemList ->
            (recyclerView.adapter as? BaseRecyclerViewAdapter<T>)?.apply {
                clear()
                addData(itemList)
            }
        }
    }

    /**
     * Use this binding adapter to show and hide the views using boolean variables
     */
    @BindingAdapter("android:fadeVisible")
    @JvmStatic
    fun setFadeVisible(view: View, visible: Boolean? = true) {
        if (view.tag == null) {
            view.tag = true
            view.visibility = if (visible == true) View.VISIBLE else View.GONE
        } else {
            view.animate().cancel()
            if (visible == true) {
                if (view.visibility == View.GONE)
                    view.fadeIn()
            } else {
                if (view.visibility == View.VISIBLE)
                    view.fadeOut()
            }
        }
    }

    @BindingAdapter(value = ["latitude", "longitude"])
    @JvmStatic
    fun setBackground(view: Button, latitude: Double?, longitude: Double?){
        view.setBackgroundColor(
            if (latitude != null && longitude!= null) ContextCompat.getColor(view.context, R.color.colorAccent) else ContextCompat.getColor(view.context, R.color.colorDisabled)
        )
        view.setTextColor(
            if (latitude != null && longitude!= null) ContextCompat.getColor(view.context, R.color.white) else ContextCompat.getColor(view.context, R.color.black)
        )
        view.isClickable = latitude != null && longitude != null
    }
}
