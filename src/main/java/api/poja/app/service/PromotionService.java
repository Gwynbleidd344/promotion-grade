package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.entity.StudentGroupPromotion;
import api.poja.app.mapper.PromotionMapper;
import api.poja.app.model.Promotion;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentGroupPromotionRepository;
import api.poja.app.repository.StudentGroupRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PromotionService {

  private final PromotionRepository promotionRepository;
  private final StudentGroupRepository studentGroupRepository;
  private final StudentGroupPromotionRepository studentGroupPromotionRepository;

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

  @Transactional
  public void addGroup(UUID promotionId, UUID groupId) {
    var promotion =
        promotionRepository
            .findById(promotionId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));
    var group =
        studentGroupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

    if (studentGroupPromotionRepository.existsByGroupIdAndPromotionId(groupId, promotionId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This group is already linked to this promotion");
    }

    var link = new StudentGroupPromotion();
    link.setPromotion(promotion);
    link.setGroup(group);
    studentGroupPromotionRepository.save(link);
  }
}
