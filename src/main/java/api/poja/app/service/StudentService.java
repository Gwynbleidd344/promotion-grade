package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.StudentGroupChangeRequest;
import api.poja.app.endpoint.rest.dto.StudentPromotionAndGroupChangeRequest;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.mapper.StudentGroupHistoryMapper;
import api.poja.app.mapper.StudentMapper;
import api.poja.app.model.Student;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentGroupHistoryRepository;
import api.poja.app.repository.StudentRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class StudentService {

  private final StudentRepository studentRepository;
  private final PromotionRepository promotionRepository;
  private final StudentGroupAssignmentService studentGroupAssignmentService;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;

  @Transactional(readOnly = true)
  public List<Student> list(UUID promotionId, ProgramCode programCode, int page, int size) {
    return studentRepository
        .findAllFiltered(promotionId, programCode, PageRequest.of(page, size))
        .map(StudentMapper::toModel)
        .getContent();
  }

  @Transactional(readOnly = true)
  public List<api.poja.app.model.StudentGroupHistory> getGroupHistory(
      UUID studentId, UUID academicYearId) {
    findStudentOrThrow(studentId);

    var entries =
        academicYearId == null
            ? studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(studentId)
            : studentGroupHistoryRepository.findByStudentIdAndAcademicYearId(
                studentId, academicYearId);

    return entries.stream()
        .sorted(
            java.util.Comparator.comparing(api.poja.app.entity.StudentGroupHistory::getStartDate))
        .map(StudentGroupHistoryMapper::toModel)
        .toList();
  }

  @Transactional(readOnly = true)
  public Student getById(UUID id) {
    return StudentMapper.toModel(findStudentOrThrow(id));
  }

  @Transactional
  public Student changePromotionAndGroup(
      UUID studentId, StudentPromotionAndGroupChangeRequest request) {
    var entity = findStudentOrThrow(studentId);
    var promotion =
        promotionRepository
            .findById(request.promotionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown promotion: " + request.promotionId()));
    var group =
        studentGroupAssignmentService.findGroupBelongingToPromotionOrThrow(
            request.groupId(), request.promotionId());

    entity.setPromotion(promotion);
    studentRepository.save(entity);

    studentGroupAssignmentService.appendHistoryEntry(
        entity, group, request.academicYearId(), request.semester(), request.startDate());

    return StudentMapper.toModel(entity);
  }

  @Transactional
  public Student changeGroup(UUID studentId, StudentGroupChangeRequest request) {
    var entity = findStudentOrThrow(studentId);
    var group =
        studentGroupAssignmentService.findGroupBelongingToPromotionOrThrow(
            request.groupId(), entity.getPromotion().getId());

    studentGroupAssignmentService.appendHistoryEntry(
        entity, group, request.academicYearId(), request.semester(), request.startDate());

    return StudentMapper.toModel(entity);
  }

  private api.poja.app.entity.Student findStudentOrThrow(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
  }
}
