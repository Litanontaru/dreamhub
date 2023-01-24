package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinServletRequest
import org.dmg.dreamhubfront.SettingController
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import javax.annotation.security.PermitAll


@Route("")
@PermitAll
class MainView(
  userSession: UserSession,
  settingController: SettingController
) : VerticalLayout() {
  init {
    val div = Div()
    div.setText("Hello ${userSession.user.firstName} ${userSession.user.lastName}")
    div.getElement().getStyle().set("font-size", "xx-large")
    val image = Image(userSession.user.picture, "User Image")

    val logoutButton = Button("Logout") { click ->
      UI.getCurrent().page.setLocation(LOGOUT_SUCCESS_URL)
      val logoutHandler = SecurityContextLogoutHandler()
      logoutHandler.logout(
        VaadinServletRequest.getCurrent().httpServletRequest, null,
        null
      )
    }
    alignItems = FlexComponent.Alignment.CENTER
    add(div, image, logoutButton)

    settingController.getAllSettings().map { Anchor("/settings/${it.id}/-1", it.name) }.forEach(::add)

    add(Button("Создать игровой мир") {
      SettingEditDialog(null) {
        settingController.addSetting(it)
      }.open()
    })
  }

  companion object {
    private const val LOGOUT_SUCCESS_URL = "/"
  }
}