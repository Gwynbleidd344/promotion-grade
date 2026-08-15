package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.endpoint.rest.dto.PromotionResponse;
import api.poja.app.entity.Promotion;
import api.poja.app.repository.PromotionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PromotionService {

  private final PromotionRepository promotionRepository;

  public List<PromotionResponse> list(int page, int size) {
    var all = promotionRepository.findAllByOrderByGraduationYearDesc();
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    return all.subList(from, to).stream().map(PromotionResponse::from).toList();
  }

  public PromotionResponse create(PromotionCreateRequest request) {
    if (promotionRepository.findByGraduationYear(request.graduationYear()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A promotion for graduation year " + request.graduationYear() + " already exists");
    }

    var promotion = new Promotion();
    promotion.setName(request.name());
    promotion.setGraduationYear(request.graduationYear());

    return PromotionResponse.from(promotionRepository.save(promotion));
  }
}
