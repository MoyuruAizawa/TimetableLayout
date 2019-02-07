package io.moyuru.timetablelayoutsample.item

import com.xwray.groupie.databinding.BindableItem
import io.moyuru.timetablelayoutsample.R
import io.moyuru.timetablelayoutsample.databinding.ItemProgramBinding
import io.moyuru.timetablelayoutsample.model.Program

class ProgramItem(private val program: Program) : BindableItem<ItemProgramBinding>() {
  override fun getLayout() = R.layout.item_program

  override fun bind(viewBinding: ItemProgramBinding, position: Int) {
    viewBinding.program = program
  }
}