package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.Promotion;
import java.util.UUID;

public record PromotionResponse(UUID id, String name, Integer graduationYear) {

  public static PromotionResponse from(Promotion promotion) {
    return new PromotionResponse(
        promotion.getId(), promotion.getName(), promotion.getGraduationYear());
  }
}
