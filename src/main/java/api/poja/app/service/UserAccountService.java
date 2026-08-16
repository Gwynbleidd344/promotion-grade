package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.PromoteAdminRequest;
import api.poja.app.endpoint.rest.dto.PromoteStudentRequest;
import api.poja.app.endpoint.rest.dto.PromoteTeacherRequest;
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
  private final StudentGroupAssignmentService studentGroupAssignmentService;

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
  public api.poja.app.model.UserAccount promoteToStudent(UUID id, PromoteStudentRequest request) {
    var userAccount = findUserAccountOrThrow(id);
    if (studentRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "This account is already a student");
    }

    var program =
        programRepository
            .findByCode(request.program())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unknown program: " + request.program()));
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

    var student = new Student();
    student.setUserAccount(userAccount);
    student.setStudentNumber(nextStudentNumber());
    student.setFirstName(request.firstName());
    student.setLastName(request.lastName());
    student.setProgram(program);
    student.setPromotion(promotion);
    student = studentRepository.save(student);

    studentGroupAssignmentService.createInitialHistory(
        student, group, request.academicYearId(), request.semester(), request.startDate());

    userAccount.setRole(UserRole.STD);
    return UserAccountMapper.toModel(userAccountRepository.save(userAccount));
  }

  @Transactional
  public api.poja.app.model.UserAccount promoteToTeacher(UUID id, PromoteTeacherRequest request) {
    var userAccount = findUserAccountOrThrow(id);
    if (teacherRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "This account is already a teacher");
    }

    var teacher = new Teacher();
    teacher.setUserAccount(userAccount);
    teacher.setEmployeeNumber(nextEmployeeNumber());
    teacher.setFirstName(request.firstName());
    teacher.setLastName(request.lastName());
    teacherRepository.save(teacher);

    userAccount.setRole(UserRole.TEC);
    return UserAccountMapper.toModel(userAccountRepository.save(userAccount));
  }

  @Transactional
  public api.poja.app.model.UserAccount promoteToAdmin(UUID id, PromoteAdminRequest request) {
    var userAccount = findUserAccountOrThrow(id);
    if (adminRepository.findByUserAccountId(userAccount.getId()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "This account is already an admin");
    }

    var admin = new Admin();
    admin.setUserAccount(userAccount);
    admin.setAdminNumber(nextAdminNumber());
    admin.setFirstName(request.firstName());
    admin.setLastName(request.lastName());
    adminRepository.save(admin);

    userAccount.setRole(UserRole.ADM);
    return UserAccountMapper.toModel(userAccountRepository.save(userAccount));
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
}
