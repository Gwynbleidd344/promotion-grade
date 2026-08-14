package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.RoleChangeRequest;
import api.poja.app.endpoint.rest.dto.UserAccountResponse;
import api.poja.app.entity.Student;
import api.poja.app.entity.Teacher;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.repository.ProgramRepository;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentRepository;
import api.poja.app.repository.TeacherRepository;
import api.poja.app.repository.UserAccountRepository;
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
public class UserAccountService {

  private final UserAccountRepository userAccountRepository;
  private final StudentRepository studentRepository;
  private final TeacherRepository teacherRepository;
  private final ProgramRepository programRepository;
  private final PromotionRepository promotionRepository;

  public List<UserAccountResponse> list(UserRole role, int page, int size) {
    var pageable = PageRequest.of(page, size);
    var result =
        role == null
            ? userAccountRepository.findAll(pageable)
            : userAccountRepository.findByRole(role, pageable);
    return result.map(UserAccountResponse::from).getContent();
  }

  public UserAccountResponse getById(UUID id) {
    return UserAccountResponse.from(findUserAccountOrThrow(id));
  }

  @Transactional
  public UserAccountResponse changeRole(UUID id, RoleChangeRequest request) {
    var userAccount = findUserAccountOrThrow(id);

    switch (request.role()) {
      case STD -> assignStudentProfileIfMissing(userAccount, request.studentProfile());
      case TEC -> assignTeacherProfileIfMissing(userAccount, request.teacherProfile());
      case ADM -> {}
    }

    userAccount.setRole(request.role());
    return UserAccountResponse.from(userAccountRepository.save(userAccount));
  }

  private void assignStudentProfileIfMissing(
      UserAccount userAccount, RoleChangeRequest.StudentProfile profile) {
    if (studentRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      return;
    }
    if (profile == null
        || isBlank(profile.studentNumber())
        || isBlank(profile.firstName())
        || isBlank(profile.lastName())
        || profile.program() == null
        || profile.promotionId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "studentProfile (studentNumber, firstName, lastName, program, promotionId) is "
              + "required for a first-time promotion to STD");
    }
    if (studentRepository.findByStudentNumber(profile.studentNumber()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student number already taken");
    }

    var program =
        programRepository
            .findByCode(profile.program())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown program: " + profile.program()));
    var promotion =
        promotionRepository
            .findById(profile.promotionId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown promotion: " + profile.promotionId()));

    var student = new Student();
    student.setUserAccount(userAccount);
    student.setStudentNumber(profile.studentNumber());
    student.setFirstName(profile.firstName());
    student.setLastName(profile.lastName());
    student.setProgram(program);
    student.setPromotion(promotion);
    studentRepository.save(student);
  }

  private void assignTeacherProfileIfMissing(
      UserAccount userAccount, RoleChangeRequest.TeacherProfile profile) {
    if (teacherRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      return;
    }
    if (profile == null
        || isBlank(profile.employeeNumber())
        || isBlank(profile.firstName())
        || isBlank(profile.lastName())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "teacherProfile (employeeNumber, firstName, lastName) is required for a "
              + "first-time promotion to TEC");
    }
    if (teacherRepository.findByEmployeeNumber(profile.employeeNumber()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee number already taken");
    }

    var teacher = new Teacher();
    teacher.setUserAccount(userAccount);
    teacher.setEmployeeNumber(profile.employeeNumber());
    teacher.setFirstName(profile.firstName());
    teacher.setLastName(profile.lastName());
    teacherRepository.save(teacher);
  }

  private UserAccount findUserAccountOrThrow(UUID id) {
    return userAccountRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
