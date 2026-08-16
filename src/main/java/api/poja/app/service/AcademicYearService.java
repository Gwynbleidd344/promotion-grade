package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.AcademicYearCreateRequest;
import api.poja.app.mapper.AcademicYearMapper;
import api.poja.app.model.AcademicYear;
import api.poja.app.repository.AcademicYearRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class AcademicYearService {

  private final AcademicYearRepository academicYearRepository;

  public List<AcademicYear> list(int page, int size) {
    var all = academicYearRepository.findAllByOrderByStartDateAsc();
    int from = Math.min(page * size, all.size());
    int to = Math.min(from + size, all.size());
    return all.subList(from, to).stream().map(AcademicYearMapper::toModel).toList();
  }

  public AcademicYear create(AcademicYearCreateRequest request) {
    if (!request.endDate().isAfter(request.startDate())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "endDate must be strictly after startDate");
    }
    if (academicYearRepository.findByLabel(request.label()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "An academic year with label '" + request.label() + "' already exists");
    }
    if (overlapsExistingYear(request)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "This date range overlaps an existing academic year");
    }

    var toCreate =
        AcademicYear.builder()
            .label(request.label())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .build();

    var saved = academicYearRepository.save(AcademicYearMapper.toNewEntity(toCreate));
    return AcademicYearMapper.toModel(saved);
  }

  private boolean overlapsExistingYear(AcademicYearCreateRequest request) {
    return academicYearRepository.findAllByOrderByStartDateAsc().stream()
        .anyMatch(
            existing ->
                !request.endDate().isBefore(existing.getStartDate())
                    && !request.startDate().isAfter(existing.getEndDate()));
  }
}
