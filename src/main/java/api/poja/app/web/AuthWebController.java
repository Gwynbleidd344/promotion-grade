package api.poja.app.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthWebController {

  @GetMapping("/login")
  public String loginPage() {
    return "login";
  }
}
