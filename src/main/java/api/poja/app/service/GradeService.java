package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.GradeCreateRequest;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.mapper.GradeMapper;
import api.poja.app.model.Grade;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.StudentRepository;
import api.poja.app.repository.TeacherRepository;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class GradeService {

  private final GradeRepository gradeRepository;
  private final ExamRepository examRepository;
  private final StudentRepository studentRepository;
  private final TeacherRepository teacherRepository;

  @Transactional
  public Grade create(UUID examId, GradeCreateRequest request) {
    var exam =
        examRepository
            .findById(examId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

    checkTeachesCourseOrAdmin(exam.getGroupCourse().getId());

    var student =
        studentRepository
            .findById(request.studentId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    if (gradeRepository.findByStudentIdAndExamId(student.getId(), examId).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "A grade already exists for this student on this exam");
    }

    var entity = GradeMapper.toNewEntity(student, exam, request.value());
    return GradeMapper.toModel(gradeRepository.save(entity));
  }

  private void checkTeachesCourseOrAdmin(UUID groupCourseId) {
    var user = currentUser();
    if (user.getRole() == UserRole.ADM) {
      return;
    }
    var teacher = teacherRepository.findByUserAccountId(user.getId()).orElse(null);
    boolean teachesCourse =
        teacher != null
            && teacher.getCourseAssignments().stream()
                .anyMatch(ct -> ct.getGroupCourse().getId().equals(groupCourseId));
    if (!teachesCourse) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "The teacher does not teach this course");
    }
  }

  private UserAccount currentUser() {
    return (UserAccount) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
