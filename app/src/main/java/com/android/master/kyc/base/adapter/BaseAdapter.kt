package com.vsmart.android.base.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class BaseAdapter<M>(diffCallback: DiffUtil.ItemCallback<M?>) :
    ListAdapter<M, BaseViewHolder<M>>(diffCallback) {

    abstract fun contentViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<M>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        contentViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: BaseViewHolder<M>, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }
}