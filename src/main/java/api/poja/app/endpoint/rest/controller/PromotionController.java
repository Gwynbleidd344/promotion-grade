package api.poja.app.endpoint.rest.controller;

import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.model.Promotion;
import api.poja.app.service.PromotionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  public ResponseEntity<List<Promotion>> listPromotions(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return ResponseEntity.ok(promotionService.list(page, size));
  }

  @PostMapping
  public ResponseEntity<Promotion> createPromotion(
      @Valid @RequestBody PromotionCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
  }

  @PostMapping("/{id}/groups/{groupId}")
  public ResponseEntity<Void> addGroupToPromotion(
      @PathVariable UUID id, @PathVariable UUID groupId) {
    promotionService.addGroup(id, groupId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
