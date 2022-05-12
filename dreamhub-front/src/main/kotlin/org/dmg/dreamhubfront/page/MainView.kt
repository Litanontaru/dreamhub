package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.html.Label
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import org.dmg.dreamhubfront.ItemController
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

@Route
class MainView : VerticalLayout() {
  @Autowired
  lateinit var api: ItemController

  @PostConstruct
  fun postConstruct() {
    add(Label("Hello! ${api.getAll(0, null, null).size}"))
  }
}