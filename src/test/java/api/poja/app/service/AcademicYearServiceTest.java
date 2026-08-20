package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.AcademicYearCreateRequest;
import api.poja.app.entity.AcademicYear;
import api.poja.app.repository.AcademicYearRepository;
import java.time.LocalDate;
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
class AcademicYearServiceTest {

  @Mock private AcademicYearRepository academicYearRepository;

  private AcademicYearService service;

  @BeforeEach
  void setUp() {
    service = new AcademicYearService(academicYearRepository);
  }

  private AcademicYear year(String label, LocalDate start, LocalDate end) {
    var entity = new AcademicYear();
    entity.setId(UUID.randomUUID());
    entity.setLabel(label);
    entity.setStartDate(start);
    entity.setEndDate(end);
    return entity;
  }

  @Test
  void lists_years_with_pagination_applied_in_memory() {
    var years =
        List.of(
            year("2023-2024", LocalDate.of(2023, 9, 1), LocalDate.of(2024, 6, 30)),
            year("2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30)),
            year("2025-2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 6, 30)));
    when(academicYearRepository.findAllByOrderByStartDateAsc()).thenReturn(years);

    var result = service.list(1, 2);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLabel()).isEqualTo("2025-2026");
  }

  @Test
  void list_returns_empty_when_page_is_beyond_available_data() {
    when(academicYearRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of());

    assertThat(service.list(0, 10)).isEmpty();
  }

  @Test
  void creates_academic_year_when_valid() {
    var request =
        new AcademicYearCreateRequest(
            "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
    when(academicYearRepository.findByLabel("2026-2027")).thenReturn(Optional.empty());
    when(academicYearRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of());
    when(academicYearRepository.save(any()))
        .thenReturn(year("2026-2027", request.startDate(), request.endDate()));

    var result = service.create(request);

    assertThat(result.getLabel()).isEqualTo("2026-2027");
    verify(academicYearRepository).save(any());
  }

  @Test
  void rejects_creation_when_end_date_is_not_after_start_date() {
    var request =
        new AcademicYearCreateRequest(
            "2026-2027", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("endDate must be strictly after startDate");
  }

  @Test
  void rejects_creation_when_label_already_exists() {
    var request =
        new AcademicYearCreateRequest(
            "2024-2025", LocalDate.of(2026, 9, 1), LocalDate.of(2027, 6, 30));
    when(academicYearRepository.findByLabel("2024-2025"))
        .thenReturn(Optional.of(year("2024-2025", LocalDate.now(), LocalDate.now().plusDays(1))));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void rejects_creation_when_date_range_overlaps_existing_year() {
    var existing = year("2024-2025", LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
    var request =
        new AcademicYearCreateRequest(
            "2025-2026", LocalDate.of(2025, 3, 1), LocalDate.of(2026, 6, 30));
    when(academicYearRepository.findByLabel("2025-2026")).thenReturn(Optional.empty());
    when(academicYearRepository.findAllByOrderByStartDateAsc()).thenReturn(List.of(existing));

    assertThatThrownBy(() -> service.create(request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("overlaps");
  }
}
