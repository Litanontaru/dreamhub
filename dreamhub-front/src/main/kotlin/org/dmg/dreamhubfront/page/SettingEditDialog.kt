package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.component.textfield.TextField
import org.dmg.dreamhubfront.SettingDto

class SettingEditDialog(
  setting: SettingDto? = null,
  save: (SettingDto) -> Unit
) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val edit = setting ?: SettingDto()
      add(TextField("Название").apply {
        value = edit.name
        addValueChangeListener { edit.name = it.value }
      })
      add(TextArea("Описание").apply {
        value = edit.description
        addValueChangeListener { edit.description = it.value }
      })
      add(Button("Ок") {
        save(edit)
        close()
      })
      add(Button("Отмена") {
        close()
      })
    })
  }
}