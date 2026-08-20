package api.poja.app.web;

import api.poja.app.web.dto.WebDtos.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Controller
public class AuthWebController {

  public static final String COOKIE_TOKEN = "accessToken";
  public static final String COOKIE_ROLE = "role";
  public static final String COOKIE_USERNAME = "username";

  private static final int COOKIE_MAX_AGE_SECONDS = 3600;

  private final RestClient apiRestClient;

  public AuthWebController(RestClient apiRestClient) {
    this.apiRestClient = apiRestClient;
  }

  @GetMapping("/login")
  public String loginPage(HttpServletRequest request) {
    if (CookieUtils.readCookie(request, COOKIE_TOKEN) != null) {
      return "redirect:/promotions";
    }
    return "login";
  }

  @PostMapping("/login")
  public String doLogin(
      @ModelAttribute("username") String username,
      @ModelAttribute("password") String password,
      HttpServletResponse response,
      Model model) {

    try {
      LoginResponse loginResponse =
          apiRestClient
              .post()
              .uri("/auth/login")
              .body(new LoginRequestBody(username, password))
              .retrieve()
              .body(LoginResponse.class);

      if (loginResponse == null || loginResponse.accessToken() == null) {
        model.addAttribute("error", "Identifiants invalides.");
        return "login";
      }

      int maxAge =
          loginResponse.expiresIn() != null ? loginResponse.expiresIn() : COOKIE_MAX_AGE_SECONDS;

      CookieUtils.setCookie(response, COOKIE_TOKEN, loginResponse.accessToken(), maxAge);
      CookieUtils.setCookie(
          response, COOKIE_ROLE, loginResponse.role() != null ? loginResponse.role() : "", maxAge);
      CookieUtils.setCookie(response, COOKIE_USERNAME, username, maxAge);

      return "redirect:/promotions";

    } catch (HttpClientErrorException.Unauthorized e) {
      model.addAttribute("error", "Nom d'utilisateur ou mot de passe incorrect.");
      return "login";
    } catch (Exception e) {
      model.addAttribute("error", "Impossible de contacter le serveur : " + e.getMessage());
      return "login";
    }
  }

  @PostMapping("/logout")
  public String logout(HttpServletResponse response) {
    CookieUtils.clearCookie(response, COOKIE_TOKEN);
    CookieUtils.clearCookie(response, COOKIE_ROLE);
    CookieUtils.clearCookie(response, COOKIE_USERNAME);
    return "redirect:/login";
  }

  private record LoginRequestBody(String username, String password) {}
}
