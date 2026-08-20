package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.rest.dto.PromoteAdminRequest;
import api.poja.app.endpoint.rest.dto.PromoteStudentRequest;
import api.poja.app.endpoint.rest.dto.PromoteTeacherRequest;
import api.poja.app.entity.Admin;
import api.poja.app.entity.Program;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.Student;
import api.poja.app.entity.StudentGroup;
import api.poja.app.entity.Teacher;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.repository.AdminRepository;
import api.poja.app.repository.ProgramRepository;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentRepository;
import api.poja.app.repository.TeacherRepository;
import api.poja.app.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private TeacherRepository teacherRepository;
  @Mock private AdminRepository adminRepository;
  @Mock private ProgramRepository programRepository;
  @Mock private PromotionRepository promotionRepository;
  @Mock private SequentialCodeGenerator sequentialCodeGenerator;
  @Mock private StudentGroupAssignmentService studentGroupAssignmentService;

  private UserAccountService service;

  private UUID accountId;
  private UUID promotionId;
  private UUID groupId;
  private UUID academicYearId;

  @BeforeEach
  void setUp() {
    service =
        new UserAccountService(
            userAccountRepository,
            studentRepository,
            teacherRepository,
            adminRepository,
            programRepository,
            promotionRepository,
            sequentialCodeGenerator,
            studentGroupAssignmentService);
    accountId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    academicYearId = UUID.randomUUID();
  }

  private UserAccount account() {
    var a = new UserAccount();
    a.setId(accountId);
    a.setUsername("jean.rakoto");
    a.setEmail("jean.rakoto@hei.mg");
    a.setPasswordHash("hash");
    return a;
  }

  @Test
  void gets_account_by_id() {
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));

    var result = service.getById(accountId);

    assertThat(result.getUsername()).isEqualTo("jean.rakoto");
  }

  @Test
  void rejects_get_by_id_when_account_not_found() {
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(accountId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User account not found");
  }

  @Test
  void promotes_account_to_student() {
    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            ProgramCode.EL,
            promotionId,
            groupId,
            academicYearId,
            1,
            LocalDate.of(2025, 9, 1));

    var program = new Program();
    program.setId(UUID.randomUUID());
    program.setCode(ProgramCode.EL);

    var promotion = new Promotion();
    promotion.setId(promotionId);

    var group = new StudentGroup();
    group.setId(groupId);

    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(studentRepository.findByUserAccountId(accountId)).thenReturn(Optional.empty());
    when(programRepository.findByCode(ProgramCode.EL)).thenReturn(Optional.of(program));
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promotion));
    when(studentGroupAssignmentService.findGroupBelongingToPromotionOrThrow(groupId, promotionId))
        .thenReturn(group);
    when(studentRepository.count()).thenReturn(0L);
    when(sequentialCodeGenerator.generate(eq("STD"), eq(0L), any())).thenReturn("STD001");
    when(studentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Student s = invocation.getArgument(0);
              s.setId(UUID.randomUUID());
              return s;
            });
    when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.promoteToStudent(accountId, request);

    assertThat(result.getRole()).isEqualTo(UserRole.STD);
  }

  @Test
  void rejects_promotion_to_student_when_already_a_student() {
    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            ProgramCode.EL,
            promotionId,
            groupId,
            academicYearId,
            1,
            LocalDate.of(2025, 9, 1));
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(studentRepository.findByUserAccountId(accountId)).thenReturn(Optional.of(new Student()));

    assertThatThrownBy(() -> service.promoteToStudent(accountId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already a student");
  }

  @Test
  void rejects_promotion_to_student_when_program_unknown() {
    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            ProgramCode.TN,
            promotionId,
            groupId,
            academicYearId,
            1,
            LocalDate.of(2025, 9, 1));
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(studentRepository.findByUserAccountId(accountId)).thenReturn(Optional.empty());
    when(programRepository.findByCode(ProgramCode.TN)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.promoteToStudent(accountId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unknown program");
  }

  @Test
  void promotes_account_to_teacher() {
    var request = new PromoteTeacherRequest("Marie", "Rasoa");
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(teacherRepository.findByUserAccountId(accountId)).thenReturn(Optional.empty());
    when(teacherRepository.count()).thenReturn(0L);
    when(sequentialCodeGenerator.generate(eq("TEC"), eq(0L), any())).thenReturn("TEC001");
    when(teacherRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Teacher t = invocation.getArgument(0);
              t.setId(UUID.randomUUID());
              return t;
            });
    when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.promoteToTeacher(accountId, request);

    assertThat(result.getRole()).isEqualTo(UserRole.TEC);
  }

  @Test
  void rejects_promotion_to_teacher_when_already_a_teacher() {
    var request = new PromoteTeacherRequest("Marie", "Rasoa");
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(teacherRepository.findByUserAccountId(accountId)).thenReturn(Optional.of(new Teacher()));

    assertThatThrownBy(() -> service.promoteToTeacher(accountId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already a teacher");
  }

  @Test
  void promotes_account_to_admin() {
    var request = new PromoteAdminRequest("Paul", "Andria");
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(adminRepository.findByUserAccountId(accountId)).thenReturn(Optional.empty());
    when(adminRepository.count()).thenReturn(0L);
    when(sequentialCodeGenerator.generate(eq("ADM"), eq(0L), any())).thenReturn("ADM001");
    when(adminRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Admin a = invocation.getArgument(0);
              a.setId(UUID.randomUUID());
              return a;
            });
    when(userAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.promoteToAdmin(accountId, request);

    assertThat(result.getRole()).isEqualTo(UserRole.ADM);
  }

  @Test
  void rejects_promotion_to_admin_when_already_an_admin() {
    var request = new PromoteAdminRequest("Paul", "Andria");
    when(userAccountRepository.findById(accountId)).thenReturn(Optional.of(account()));
    when(adminRepository.findByUserAccountId(accountId)).thenReturn(Optional.of(new Admin()));

    assertThatThrownBy(() -> service.promoteToAdmin(accountId, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("already an admin");
  }
}
