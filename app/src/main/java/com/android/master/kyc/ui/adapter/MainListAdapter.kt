package com.android.master.kyc.ui.adapter

import android.animation.ValueAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.master.kyc.R
import com.android.master.kyc.databinding.ItemListBinding
import com.android.master.kyc.extension.blendColors
import com.android.master.kyc.extension.dp
import com.android.master.kyc.extension.getValueAnimator
import com.android.master.kyc.extension.screenWidth
import com.android.master.kyc.model.Category
import com.android.master.kyc.ui.animationPlaybackSpeed

typealias ItemOnClick = (Int) -> Unit

class MainListAdapter(
    private val context: Context,
    private val list: List<Category>,
    private val onClick: ItemOnClick
) : RecyclerView.Adapter<MainListAdapter.ListViewHolder>() {

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val expandView: View = itemView.findViewById(R.id.expand_view)
        val chevron: View = itemView.findViewById(R.id.chevron)
        val cardContainer: View = itemView.findViewById(R.id.card_container)
        val scaleContainer: View = itemView.findViewById(R.id.scale_container)
        val listItemFg: View = itemView.findViewById(R.id.list_item_fg)

        val categoryLD = MutableLiveData<String>()
    }

    private val originalBg: Int by lazy {
        ContextCompat.getColor(context, R.color.list_item_bg_collapsed)
    }
    private val expandedBg: Int by lazy {
        ContextCompat.getColor(context, R.color.list_item_bg_expanded)
    }

    private val listItemHorizontalPadding: Float by lazy {
        context.resources.getDimension(R.dimen.list_item_horizontal_padding)
    }

    private val listItemVerticalPadding: Float by lazy {
        context.resources.getDimension(R.dimen.list_item_vertical_padding)
    }

    private val originalWidth = context.screenWidth - 48.dp
    private val expandedWidth = context.screenWidth - 24.dp
    private var originalHeight = -1 // will be calculated dynamically
    private var expandedHeight = -1 // will be calculated dynamically

    private val listItemExpandDuration: Long get() = (300L / animationPlaybackSpeed).toLong()

    private lateinit var recyclerView: RecyclerView
    private var expandedModel: Category? = null
    private var isScaledDown = false

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val lifecycleOwner = parent.context as LifecycleOwner
        val binding = DataBindingUtil.inflate<ItemListBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_list,
            parent,
            false
        )
        val vh = ListViewHolder(binding.root)

        binding.let {
            it.setLifecycleOwner(lifecycleOwner)
            it.vh = vh
        }

        vh.categoryLD.observe(lifecycleOwner, Observer {
            val user = list[vh.layoutPosition]
            user.title = it ?: ""
        })

        return vh
    }


    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int): Unit = with(holder) {
        val model = list[position]

        categoryLD.postValue(model.title)

        expandItem(holder, model == expandedModel, animate = false)
        scaleDownItem(holder, position, isScaledDown)

        holder.cardContainer.setOnClickListener {
            if (expandedModel == null) {
                // expand clicked view
                expandItem(holder, expand = true, animate = true)
                expandedModel = model
            } else if (expandedModel == model) {
                // collapse clicked view
                expandItem(holder, expand = false, animate = true)
                expandedModel = null
            } else {
                // collapse previously expanded view
                val expandedModelPosition = list.indexOf(expandedModel!!)
                val oldViewHolder =
                    recyclerView.findViewHolderForAdapterPosition(expandedModelPosition) as? ListViewHolder
                if (oldViewHolder != null) expandItem(oldViewHolder, expand = false, animate = true)

                // expand clicked view
                expandItem(holder, expand = true, animate = true)
                expandedModel = model
            }
        }

        holder.expandView.setOnClickListener {
            onClick(position)
        }
    }

    private fun expandItem(holder: ListViewHolder, expand: Boolean, animate: Boolean) {
        if (animate) {
            val animator = getValueAnimator(
                expand, listItemExpandDuration, AccelerateDecelerateInterpolator()
            ) { progress -> setExpandProgress(holder, progress) }

            if (expand) animator.doOnStart { holder.expandView.isVisible = true }
            else animator.doOnEnd { holder.expandView.isVisible = false }

            animator.start()
        } else {
            // show expandView only if we have expandedHeight (onViewAttached)
            holder.expandView.isVisible = expand && expandedHeight >= 0
            setExpandProgress(holder, if (expand) 1f else 0f)
        }
    }

    override fun onViewAttachedToWindow(holder: ListViewHolder) {
        super.onViewAttachedToWindow(holder)

        // get originalHeight & expandedHeight if not gotten before
        if (expandedHeight < 0) {
            expandedHeight = 0 // so that this block is only called once

            holder.cardContainer.doOnLayout { view ->
                originalHeight = view.height

                // show expandView and record expandedHeight in next layout pass
                // (doOnPreDraw) and hide it immediately. We use onPreDraw because
                // it's called after layout is done. doOnNextLayout is called during
                // layout phase which causes issues with hiding expandView.
                holder.expandView.isVisible = true
                view.doOnPreDraw {
                    expandedHeight = view.height
                    holder.expandView.isVisible = false
                }
            }
        }
    }

    private fun setExpandProgress(holder: ListViewHolder, progress: Float) {
        if (expandedHeight > 0 && originalHeight > 0) {
            holder.cardContainer.layoutParams.height =
                (originalHeight + (expandedHeight - originalHeight) * progress).toInt()
        }
        holder.cardContainer.layoutParams.width =
            (originalWidth + (expandedWidth - originalWidth) * progress).toInt()

        holder.cardContainer.setBackgroundColor(blendColors(originalBg, expandedBg, progress))
        holder.cardContainer.requestLayout()

        holder.chevron.rotation = 90 * progress
    }

    ///////////////////////////////////////////////////////////////////////////
    // Scale Down Animation
    ///////////////////////////////////////////////////////////////////////////

    private inline val LinearLayoutManager.visibleItemsRange: IntRange
        get() = findFirstVisibleItemPosition()..findLastVisibleItemPosition()

    fun getScaleDownAnimator(isScaledDown: Boolean): ValueAnimator {
        val lm = recyclerView.layoutManager as LinearLayoutManager

        val animator = getValueAnimator(
            isScaledDown,
            duration = 300L, interpolator = AccelerateDecelerateInterpolator()
        ) { progress ->

            // Get viewHolder for all visible items and animate attributes
            for (i in lm.visibleItemsRange) {
                val holder = recyclerView.findViewHolderForLayoutPosition(i) as ListViewHolder
                setScaleDownProgress(holder, i, progress)
            }
        }

        // Set adapter variable when animation starts so that newly binded views in
        // onBindViewHolder will respect the new size when they come into the screen
        animator.doOnStart { this.isScaledDown = isScaledDown }

        // For all the non visible items in the layout manager, notify them to adjust the
        // view to the new size
        animator.doOnEnd {
            repeat(lm.itemCount) { if (it !in lm.visibleItemsRange) notifyItemChanged(it) }
        }
        return animator
    }

    private fun setScaleDownProgress(holder: ListViewHolder, position: Int, progress: Float) {
        val itemExpanded = position >= 0 && list[position] == expandedModel
        holder.cardContainer.layoutParams.apply {
            width =
                ((if (itemExpanded) expandedWidth else originalWidth) * (1 - 0.1f * progress)).toInt()
            height =
                ((if (itemExpanded) expandedHeight else originalHeight) * (1 - 0.1f * progress)).toInt()
        }
        holder.cardContainer.requestLayout()

        holder.scaleContainer.scaleX = 1 - 0.05f * progress
        holder.scaleContainer.scaleY = 1 - 0.05f * progress

        holder.scaleContainer.setPadding(
            (listItemHorizontalPadding * (1 - 0.2f * progress)).toInt(),
            (listItemVerticalPadding * (1 - 0.2f * progress)).toInt(),
            (listItemHorizontalPadding * (1 - 0.2f * progress)).toInt(),
            (listItemVerticalPadding * (1 - 0.2f * progress)).toInt()
        )

        holder.listItemFg.alpha = progress
    }

    private fun scaleDownItem(holder: ListViewHolder, position: Int, isScaleDown: Boolean) {
        setScaleDownProgress(holder, position, if (isScaleDown) 1f else 0f)
    }
}