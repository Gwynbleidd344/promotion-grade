package api.poja.app.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String token = CookieUtils.readCookie(request, AuthWebController.COOKIE_TOKEN);
    if (token == null) {
      response.sendRedirect(request.getContextPath() + "/login");
      return false;
    }
    return true;
  }
}
