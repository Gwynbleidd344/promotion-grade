package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.ExamCreateRequest;
import api.poja.app.mapper.ExamMapper;
import api.poja.app.model.Exam;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GroupCourseRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class ExamService {

  private final ExamRepository examRepository;
  private final GroupCourseRepository groupCourseRepository;

  @Transactional(readOnly = true)
  public List<Exam> list(UUID courseAssignmentId) {
    findGroupCourseOrThrow(courseAssignmentId);
    return examRepository.findByGroupCourseId(courseAssignmentId).stream()
        .map(ExamMapper::toModel)
        .toList();
  }

  @Transactional
  public Exam create(UUID courseAssignmentId, ExamCreateRequest request) {
    var groupCourse = findGroupCourseOrThrow(courseAssignmentId);

    var existingCoefficientSum =
        examRepository.findByGroupCourseId(courseAssignmentId).stream()
            .map(api.poja.app.entity.Exam::getCoefficient)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    var newSum = existingCoefficientSum.add(request.coefficient());

    if (newSum.compareTo(BigDecimal.ONE) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Sum of exam coefficients for this course would exceed 1 (currently "
              + existingCoefficientSum
              + ", adding "
              + request.coefficient()
              + ")");
    }

    var toCreate =
        Exam.builder()
            .name(request.name())
            .examDate(request.examDate())
            .examTime(request.examTime())
            .coefficient(request.coefficient())
            .build();

    var entity = ExamMapper.toNewEntity(toCreate, groupCourse);
    return ExamMapper.toModel(examRepository.save(entity));
  }

  private api.poja.app.entity.GroupCourse findGroupCourseOrThrow(UUID courseAssignmentId) {
    return groupCourseRepository
        .findById(courseAssignmentId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course assignment not found"));
  }
}
