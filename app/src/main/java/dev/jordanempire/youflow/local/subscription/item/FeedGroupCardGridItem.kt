package dev.jordanempire.youflow.local.subscription.item

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import dev.jordanempire.youflow.R
import dev.jordanempire.youflow.database.feed.model.FeedGroupEntity
import dev.jordanempire.youflow.databinding.FeedGroupCardGridItemBinding
import dev.jordanempire.youflow.local.subscription.FeedGroupIcon

data class FeedGroupCardGridItem(
    val groupId: Long = FeedGroupEntity.GROUP_ALL_ID,
    val name: String,
    val icon: FeedGroupIcon
) : BindableItem<FeedGroupCardGridItemBinding>() {
    constructor (feedGroupEntity: FeedGroupEntity) : this(feedGroupEntity.uid, feedGroupEntity.name, feedGroupEntity.icon)

    override fun getId(): Long {
        return when (groupId) {
            FeedGroupEntity.GROUP_ALL_ID -> super.getId()
            else -> groupId
        }
    }

    override fun getLayout(): Int = R.layout.feed_group_card_grid_item

    override fun bind(viewBinding: FeedGroupCardGridItemBinding, position: Int) {
        viewBinding.title.text = name
        viewBinding.icon.setImageResource(icon.getDrawableRes())
    }

    override fun initializeViewBinding(view: View) = FeedGroupCardGridItemBinding.bind(view)
}
