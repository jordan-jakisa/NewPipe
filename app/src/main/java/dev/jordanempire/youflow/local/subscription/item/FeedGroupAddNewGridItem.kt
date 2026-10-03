package dev.jordanempire.youflow.local.subscription.item

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import dev.jordanempire.youflow.R
import dev.jordanempire.youflow.databinding.FeedGroupAddNewGridItemBinding

class FeedGroupAddNewGridItem : BindableItem<FeedGroupAddNewGridItemBinding>() {
    override fun getLayout(): Int = R.layout.feed_group_add_new_grid_item
    override fun initializeViewBinding(view: View) = FeedGroupAddNewGridItemBinding.bind(view)
    override fun bind(viewBinding: FeedGroupAddNewGridItemBinding, position: Int) {
        // this is a static item, nothing to do here
    }
}
