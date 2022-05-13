package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinServletRequest
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import javax.annotation.security.PermitAll


@Route("")
@PermitAll
class MainView(userSession: UserSession) : VerticalLayout() {
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
  }

  companion object {
    private const val LOGOUT_SUCCESS_URL = "/"
  }
}