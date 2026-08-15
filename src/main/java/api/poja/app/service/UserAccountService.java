package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.RoleChangeRequest;
import api.poja.app.entity.Admin;
import api.poja.app.entity.Student;
import api.poja.app.entity.Teacher;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.mapper.UserAccountMapper;
import api.poja.app.repository.AdminRepository;
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

  private static final String STUDENT_NUMBER_PREFIX = "STD";
  private static final String EMPLOYEE_NUMBER_PREFIX = "TEC";
  private static final String ADMIN_NUMBER_PREFIX = "ADM";

  private final UserAccountRepository userAccountRepository;
  private final StudentRepository studentRepository;
  private final TeacherRepository teacherRepository;
  private final AdminRepository adminRepository;
  private final ProgramRepository programRepository;
  private final PromotionRepository promotionRepository;
  private final SequentialCodeGenerator sequentialCodeGenerator;

  public List<api.poja.app.model.UserAccount> list(UserRole role, int page, int size) {
    var pageable = PageRequest.of(page, size);
    var result =
        role == null
            ? userAccountRepository.findAll(pageable)
            : userAccountRepository.findByRole(role, pageable);
    return result.map(UserAccountMapper::toModel).getContent();
  }

  public api.poja.app.model.UserAccount getById(UUID id) {
    return UserAccountMapper.toModel(findUserAccountOrThrow(id));
  }

  @Transactional
  public api.poja.app.model.UserAccount changeRole(UUID id, RoleChangeRequest request) {
    var userAccount = findUserAccountOrThrow(id);

    switch (request.role()) {
      case STD -> assignStudentProfileIfMissing(userAccount, request.studentProfile());
      case TEC -> assignTeacherProfileIfMissing(userAccount, request.teacherProfile());
      case ADM -> assignAdminProfileIfMissing(userAccount, request.adminProfile());
    }

    userAccount.setRole(request.role());
    return UserAccountMapper.toModel(userAccountRepository.save(userAccount));
  }

  private void assignStudentProfileIfMissing(
      UserAccount userAccount, RoleChangeRequest.StudentProfile profile) {
    if (studentRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      return;
    }
    if (profile == null
        || isBlank(profile.firstName())
        || isBlank(profile.lastName())
        || profile.program() == null
        || profile.promotionId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "studentProfile (firstName, lastName, program, promotionId) is "
              + "required for a first-time promotion to STD");
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
    student.setStudentNumber(nextStudentNumber());
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
    if (profile == null || isBlank(profile.firstName()) || isBlank(profile.lastName())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "teacherProfile (firstName, lastName) is required for a first-time promotion to TEC");
    }

    var teacher = new Teacher();
    teacher.setUserAccount(userAccount);
    teacher.setEmployeeNumber(nextEmployeeNumber());
    teacher.setFirstName(profile.firstName());
    teacher.setLastName(profile.lastName());
    teacherRepository.save(teacher);
  }

  private void assignAdminProfileIfMissing(
      UserAccount userAccount, RoleChangeRequest.AdminProfile profile) {
    if (adminRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      return;
    }
    if (profile == null || isBlank(profile.firstName()) || isBlank(profile.lastName())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "adminProfile (firstName, lastName) is required for a first-time promotion to ADM");
    }

    var admin = new Admin();
    admin.setUserAccount(userAccount);
    admin.setAdminNumber(nextAdminNumber());
    admin.setFirstName(profile.firstName());
    admin.setLastName(profile.lastName());
    adminRepository.save(admin);
  }

  private String nextStudentNumber() {
    return sequentialCodeGenerator.generate(
        STUDENT_NUMBER_PREFIX,
        studentRepository.count(),
        candidate -> studentRepository.findByStudentNumber(candidate).isPresent());
  }

  private String nextEmployeeNumber() {
    return sequentialCodeGenerator.generate(
        EMPLOYEE_NUMBER_PREFIX,
        teacherRepository.count(),
        candidate -> teacherRepository.findByEmployeeNumber(candidate).isPresent());
  }

  private String nextAdminNumber() {
    return sequentialCodeGenerator.generate(
        ADMIN_NUMBER_PREFIX,
        adminRepository.count(),
        candidate -> adminRepository.findByAdminNumber(candidate).isPresent());
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
