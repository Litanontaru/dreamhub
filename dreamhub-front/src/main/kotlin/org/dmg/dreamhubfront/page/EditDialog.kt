package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class EditDialog(name: String, value: String, save: (String) -> Unit) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val textField = TextField(name).also {
        it.value = value
      }
      add(textField)
      add(HorizontalLayout().apply {
        add(Button("Ок") {
          save(textField.value)
          close()
        })
        add(Button("Отмена") { close() })

        width = "100%"
      })

    })
  }
}