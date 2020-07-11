package com.vsmart.android.base.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.vsmart.android.base.widget.recyclerview.decoration.InsetDividerDecoration

abstract class BaseViewHolder<T>(view: View) : RecyclerView.ViewHolder(view), InsetDividerDecoration.HasDivider {
    abstract fun bind(item: T)
    override fun canDivide(): Boolean = true
    open fun onDetached() = Unit
}