package api.poja.app.web;

import api.poja.app.service.PromotionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class PromotionWebController {

  private final PromotionService promotionService;

  @GetMapping("/promotions-view")
  public String promotionsPage(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Model model) {
    var promotions = promotionService.list(page, size);
    model.addAttribute("promotions", promotions);
    return "promotions";
  }
}
