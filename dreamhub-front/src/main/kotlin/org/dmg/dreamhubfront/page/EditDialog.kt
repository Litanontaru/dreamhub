package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField

class EditDialog(name: String, value: String, val allowEmpty: Boolean = false, save: (String) -> Unit) : Dialog() {
  init {
    add(VerticalLayout().apply {
      val textField = TextField(name).also {
        it.value = value

        it.width = "100%"
      }
      add(textField)
      add(HorizontalLayout().apply {
        add(Button("Ок") {
          save(textField.value)
          close()
        }.apply {
          addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        })
        if (allowEmpty) {
          add(Button("Пусто") {
            save("")
            close()
          }.apply {
            addThemeVariants(ButtonVariant.LUMO_CONTRAST);
          })
        }
        add(Button("Отмена") { close() }.apply {
          addThemeVariants(ButtonVariant.LUMO_TERTIARY)
        })

        width = "100%"
      })

    })
    width = "40em"
  }
}