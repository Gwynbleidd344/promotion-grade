package api.poja.app.web;

import api.poja.app.web.dto.WebDtos.GraduationListExport;
import api.poja.app.web.dto.WebDtos.Promotion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Controller
public class PromotionWebController {

  private final RestClient apiRestClient;

  public PromotionWebController(RestClient apiRestClient) {
    this.apiRestClient = apiRestClient;
  }

  @GetMapping("/promotions")
  public String listPromotions(
      HttpServletRequest request,
      HttpServletResponse response,
      Model model,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    String token = CookieUtils.readCookie(request, AuthWebController.COOKIE_TOKEN);

    try {
      List<Promotion> promotions =
          apiRestClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/promotions")
                          .queryParam("page", page)
                          .queryParam("size", size)
                          .build())
              .header("Authorization", "Bearer " + token)
              .retrieve()
              .body(new org.springframework.core.ParameterizedTypeReference<List<Promotion>>() {});

      model.addAttribute("promotions", promotions);
      model.addAttribute(
          "username", CookieUtils.readCookie(request, AuthWebController.COOKIE_USERNAME));
      model.addAttribute("role", CookieUtils.readCookie(request, AuthWebController.COOKIE_ROLE));
      return "promotions";

    } catch (HttpClientErrorException.Unauthorized e) {
      CookieUtils.clearCookie(response, AuthWebController.COOKIE_TOKEN);
      CookieUtils.clearCookie(response, AuthWebController.COOKIE_ROLE);
      CookieUtils.clearCookie(response, AuthWebController.COOKIE_USERNAME);
      return "redirect:/login";
    } catch (HttpClientErrorException.Forbidden e) {
      model.addAttribute("error", "Acces refuse : role insuffisant pour lister les promotions.");
      return "promotions";
    } catch (Exception e) {
      model.addAttribute("error", "Erreur lors du chargement des promotions : " + e.getMessage());
      return "promotions";
    }
  }

  @GetMapping("/promotions/{id}/graduates/download")
  public String downloadGraduates(
      @PathVariable UUID id,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {

    String token = CookieUtils.readCookie(request, AuthWebController.COOKIE_TOKEN);

    try {
      GraduationListExport export =
          apiRestClient
              .get()
              .uri("/promotions/{id}/graduates/export", id)
              .header("Authorization", "Bearer " + token)
              .retrieve()
              .body(GraduationListExport.class);

      if (export == null || export.downloadUrl() == null) {
        model.addAttribute("error", "Aucun fichier disponible pour cette promotion.");
        return listPromotions(request, response, model, 0, 20);
      }

      return "redirect:" + export.downloadUrl();

    } catch (HttpClientErrorException.NotFound e) {
      model.addAttribute("error", "Promotion introuvable.");
      return listPromotions(request, response, model, 0, 20);
    } catch (HttpClientErrorException.Forbidden e) {
      model.addAttribute("error", "Acces refuse pour cette action.");
      return listPromotions(request, response, model, 0, 20);
    } catch (Exception e) {
      model.addAttribute("error", "Erreur lors de la generation du fichier : " + e.getMessage());
      return listPromotions(request, response, model, 0, 20);
    }
  }
}
