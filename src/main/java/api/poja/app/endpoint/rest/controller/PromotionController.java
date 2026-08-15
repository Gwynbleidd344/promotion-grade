package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.endpoint.rest.dto.PromotionResponse;
import api.poja.app.service.PromotionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/promotions")
@AllArgsConstructor
public class PromotionController {

  private final PromotionService promotionService;

  @GetMapping
  public ResponseEntity<List<PromotionResponse>> listPromotions(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(promotionService.list(page, size));
  }

  @PreAuthorize("hasRole('ADM')")
  @PostMapping
  public ResponseEntity<PromotionResponse> createPromotion(
      @Valid @RequestBody PromotionCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
  }
}
