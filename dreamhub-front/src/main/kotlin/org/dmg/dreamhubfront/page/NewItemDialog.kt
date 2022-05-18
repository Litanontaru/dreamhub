package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class NewItemDialog(save: (String) -> Unit) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val textField = TextField("Название")
      add(textField)
      add(Button("Ок") {
        save(textField.value)
        close()
      })
      add(Button("Отмена") { close() })
    })
  }
}