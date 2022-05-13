package org.dmg.dreamhubfront.page

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.InputStreamFactory
import com.vaadin.flow.server.StreamResource
import com.vaadin.flow.server.auth.AnonymousAllowed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment


@Route("login")
@PageTitle("Login")
@AnonymousAllowed
class LoginView(@Autowired env: Environment) : VerticalLayout() {
  init {
    val imageResource = StreamResource("dreamhub.png", InputStreamFactory { javaClass.getResourceAsStream("/images/dreamhub.png") })
    add(Image(imageResource, "Dreamhub logo"))

    isPadding = true
    alignItems = FlexComponent.Alignment.CENTER
    val clientkey = env.getProperty("spring.security.oauth2.client.registration.google.client-id")

    // Check that oauth keys are present
    if (clientkey == null || clientkey.isEmpty() || clientkey.length < 32) {
      val text = Paragraph(
        "Could not find OAuth client key in application.properties. "
            + "Please double-check the key and refer to the README.md file for instructions."
      )
      text.style["padding-top"] = "100px"
      add(text)
    } else {
      add(Button("Login with Google") {
        getUI().get().getPage().setLocation(OAUTH_URL)
      })
    }
  }

  companion object {
    /**
     * URL that Spring uses to connect to Google services
     */
    private val OAUTH_URL = "/oauth2/authorization/google"
  }
}