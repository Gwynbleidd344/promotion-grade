package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.mapper.PromotionMapper;
import api.poja.app.model.Promotion;
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

  public List<Promotion> list(int page, int size) {
    var all = promotionRepository.findAllByOrderByGraduationYearDesc();
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    return all.subList(from, to).stream().map(PromotionMapper::toModel).toList();
  }

  public Promotion create(PromotionCreateRequest request) {
    if (promotionRepository.findByGraduationYear(request.graduationYear()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A promotion for graduation year " + request.graduationYear() + " already exists");
    }

    var toCreate =
        Promotion.builder().name(request.name()).graduationYear(request.graduationYear()).build();

    var saved = promotionRepository.save(PromotionMapper.toNewEntity(toCreate));
    return PromotionMapper.toModel(saved);
  }
}
