package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.StudentGroup;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentGroupPromotionRepository;
import api.poja.app.repository.StudentGroupRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

  @Mock private PromotionRepository promotionRepository;
  @Mock private StudentGroupRepository studentGroupRepository;
  @Mock private StudentGroupPromotionRepository studentGroupPromotionRepository;

  private PromotionService service;

  private UUID promotionId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    service =
        new PromotionService(
            promotionRepository, studentGroupRepository, studentGroupPromotionRepository);
    promotionId = UUID.randomUUID();
    groupId = UUID.randomUUID();
  }

  private Promotion promotion(String name, int year) {
    var p = new Promotion();
    p.setId(promotionId);
    p.setName(name);
    p.setGraduationYear(year);
    return p;
  }

  private StudentGroup group() {
    var g = new StudentGroup();
    g.setId(groupId);
    g.setReference("K1");
    return g;
  }

  @Test
  void lists_promotions_with_pagination_applied_in_memory() {
    when(promotionRepository.findAllByOrderByGraduationYearDesc())
        .thenReturn(List.of(promotion("Promo 2027", 2027), promotion("Promo 2026", 2026)));

    var result = service.list(0, 1);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getGraduationYear()).isEqualTo(2027);
  }

  @Test
  void creates_promotion_when_graduation_year_is_unique() {
    var request = new PromotionCreateRequest("Promo 2029", 2029);
    when(promotionRepository.findByGraduationYear(2029)).thenReturn(Optional.empty());
    when(promotionRepository.save(any())).thenReturn(promotion("Promo 2029", 2029));

    var result = service.create(request);

    assertThat(result.getGraduationYear()).isEqualTo(2029);
  }

  @Test
  void rejects_creation_when_graduation_year_already_exists() {
    var request = new PromotionCreateRequest("Promo 2026", 2026);
    when(promotionRepository.findByGraduationYear(2026))
        .thenReturn(Optional.of(promotion("Promo 2026", 2026)));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void adds_group_to_promotion_when_link_does_not_exist() {
    when(promotionRepository.findById(promotionId))
        .thenReturn(Optional.of(promotion("Promo 2026", 2026)));
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(studentGroupPromotionRepository.existsByGroupIdAndPromotionId(groupId, promotionId))
        .thenReturn(false);

    service.addGroup(promotionId, groupId);

    verify(studentGroupPromotionRepository).save(any());
  }

  @Test
  void rejects_add_group_when_promotion_not_found() {
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.addGroup(promotionId, groupId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Promotion not found");
  }

  @Test
  void rejects_add_group_when_group_not_found() {
    when(promotionRepository.findById(promotionId))
        .thenReturn(Optional.of(promotion("Promo 2026", 2026)));
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.addGroup(promotionId, groupId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Group not found");
  }

  @Test
  void rejects_add_group_when_link_already_exists() {
    when(promotionRepository.findById(promotionId))
        .thenReturn(Optional.of(promotion("Promo 2026", 2026)));
    when(studentGroupRepository.findById(groupId)).thenReturn(Optional.of(group()));
    when(studentGroupPromotionRepository.existsByGroupIdAndPromotionId(groupId, promotionId))
        .thenReturn(true);

    assertThatThrownBy(() -> service.addGroup(promotionId, groupId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already linked");
  }
}
