package io.moyuru.timetablelayoutmanagersample

import com.xwray.groupie.databinding.BindableItem
import io.moyuru.timetablelayoutmanagersample.databinding.ItemSpaceBinding

class SpaceItem : BindableItem<ItemSpaceBinding>() {
  override fun getLayout() = R.layout.item_space

  override fun bind(viewBinding: ItemSpaceBinding, position: Int) {
  }
}