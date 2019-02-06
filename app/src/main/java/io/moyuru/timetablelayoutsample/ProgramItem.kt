package io.moyuru.timetablelayoutsample

import com.xwray.groupie.databinding.BindableItem
import io.moyuru.timetablelayoutsample.databinding.ItemProgramBinding

class ProgramItem(private val program: Program) : BindableItem<ItemProgramBinding>() {
  override fun getLayout() = R.layout.item_program

  override fun bind(viewBinding: ItemProgramBinding, position: Int) {
    viewBinding.program = program
  }
}