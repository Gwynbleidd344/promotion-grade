package api.poja.app.service;

import api.poja.app.entity.AcademicYear;
import api.poja.app.entity.Student;
import api.poja.app.entity.StudentGroup;
import api.poja.app.entity.StudentGroupHistory;
import api.poja.app.repository.AcademicYearRepository;
import api.poja.app.repository.StudentGroupHistoryRepository;
import api.poja.app.repository.StudentGroupPromotionRepository;
import api.poja.app.repository.StudentGroupRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class StudentGroupAssignmentService {

  private final StudentGroupRepository studentGroupRepository;
  private final StudentGroupPromotionRepository studentGroupPromotionRepository;
  private final StudentGroupHistoryRepository studentGroupHistoryRepository;
  private final AcademicYearRepository academicYearRepository;

  public StudentGroup findGroupBelongingToPromotionOrThrow(UUID groupId, UUID promotionId) {
    var group =
        studentGroupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    if (!studentGroupPromotionRepository.existsByGroupIdAndPromotionId(groupId, promotionId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Group " + groupId + " does not belong to promotion " + promotionId);
    }
    return group;
  }

  @Transactional
  public StudentGroupHistory createInitialHistory(
      Student student,
      StudentGroup group,
      UUID academicYearId,
      Integer semester,
      LocalDate startDate) {
    return saveHistoryEntry(student, group, academicYearId, semester, startDate);
  }

  @Transactional
  public StudentGroupHistory appendHistoryEntry(
      Student student,
      StudentGroup group,
      UUID academicYearId,
      Integer semester,
      LocalDate startDate) {
    closeCurrentOpenEntry(student, startDate);
    return saveHistoryEntry(student, group, academicYearId, semester, startDate);
  }

  private StudentGroupHistory saveHistoryEntry(
      Student student,
      StudentGroup group,
      UUID academicYearId,
      Integer semester,
      LocalDate startDate) {
    var academicYear = findAcademicYearOrThrow(academicYearId);
    var entry = new StudentGroupHistory();
    entry.setStudent(student);
    entry.setGroup(group);
    entry.setAcademicYear(academicYear);
    entry.setSemester(semester);
    entry.setStartDate(startDate);
    return studentGroupHistoryRepository.save(entry);
  }

  private void closeCurrentOpenEntry(Student student, LocalDate endDate) {
    studentGroupHistoryRepository.findByStudentIdOrderByStartDateAsc(student.getId()).stream()
        .filter(h -> h.getEndDate() == null)
        .max(Comparator.comparing(StudentGroupHistory::getStartDate))
        .ifPresent(
            current -> {
              current.setEndDate(endDate);
              studentGroupHistoryRepository.save(current);
            });
  }

  private AcademicYear findAcademicYearOrThrow(UUID academicYearId) {
    return academicYearRepository
        .findById(academicYearId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Academic year not found"));
  }
}
