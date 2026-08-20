package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.conf.FacadeIT;
import api.poja.app.endpoint.rest.dto.AcademicYearCreateRequest;
import api.poja.app.endpoint.rest.dto.CourseAssignmentCreateRequest;
import api.poja.app.endpoint.rest.dto.CourseCreateRequest;
import api.poja.app.endpoint.rest.dto.LoginResponse;
import api.poja.app.endpoint.rest.dto.PromoteStudentRequest;
import api.poja.app.endpoint.rest.dto.PromoteTeacherRequest;
import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.endpoint.rest.dto.RegisterRequest;
import api.poja.app.endpoint.rest.dto.StudentGroupCreateRequest;
import api.poja.app.endpoint.rest.dto.StudentProgramChangeRequest;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.mapper.StudentMapper;
import api.poja.app.model.AcademicYear;
import api.poja.app.model.Course;
import api.poja.app.model.CourseAssignment;
import api.poja.app.model.Promotion;
import api.poja.app.model.Student;
import api.poja.app.model.StudentGroup;
import api.poja.app.repository.StudentRepository;
import api.poja.app.repository.UserAccountRepository;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

public abstract class IntegrationTestSupport extends FacadeIT {

  protected static final String API = "/api/v1";
  protected static final String DEFAULT_PASSWORD = "Password123!";
  private static final AtomicInteger YEAR_COUNTER = new AtomicInteger(2000);

  @Autowired protected TestRestTemplate restTemplate;
  @Autowired protected UserAccountRepository userAccountRepository;
  @Autowired protected StudentRepository studentRepository;
  @Autowired protected PasswordEncoder passwordEncoder;

  @BeforeEach
  void baseSetUp() {
    // no-op hook for subclasses that don't need extra setup
  }

  protected String uniqueUsername(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  protected int nextYear() {
    return YEAR_COUNTER.incrementAndGet();
  }

  protected HttpHeaders authHeaders(String token) {
    var headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  protected <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
    return restTemplate.exchange(
        path, HttpMethod.GET, new HttpEntity<>(authHeaders(token)), responseType);
  }

  protected <T> ResponseEntity<T> post(
      String path, String token, Object body, Class<T> responseType) {
    return restTemplate.exchange(
        path, HttpMethod.POST, new HttpEntity<>(body, authHeaders(token)), responseType);
  }

  protected <T> ResponseEntity<T> patch(
      String path, String token, Object body, Class<T> responseType) {
    return restTemplate.exchange(
        path, HttpMethod.PATCH, new HttpEntity<>(body, authHeaders(token)), responseType);
  }

  protected UserAccount registerRaw(String username, String email, String password) {
    var request = new RegisterRequest(username, email, password);
    var response = post(API + "/auth/register", null, request, UserAccount.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  protected UserAccount register(String usernamePrefix) {
    String username = uniqueUsername(usernamePrefix);
    return registerRaw(username, username + "@hei.mg", DEFAULT_PASSWORD);
  }

  protected String login(String username, String password) {
    var request = new api.poja.app.endpoint.rest.dto.LoginRequest(username, password);
    var response = post(API + "/auth/login", null, request, LoginResponse.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody().accessToken();
  }

  protected String bootstrapAdminToken() {
    String username = uniqueUsername("admin");
    String password = DEFAULT_PASSWORD;
    var account = new UserAccount();
    account.setUsername(username);
    account.setEmail(username + "@hei.mg");
    account.setPasswordHash(passwordEncoder.encode(password));
    account.setRole(UserRole.ADM);
    account.setEnabled(true);
    userAccountRepository.save(account);
    return login(username, password);
  }

  protected record RegisteredUser(UUID id, String username, String token) {}

  protected RegisteredUser registerAndLogin(String prefix) {
    var user = register(prefix);
    var token = login(user.getUsername(), DEFAULT_PASSWORD);
    return new RegisteredUser(user.getId(), user.getUsername(), token);
  }

  protected AcademicYear createAcademicYear(String adminToken, String label) {
    int year = nextYear();
    var request =
        new AcademicYearCreateRequest(
            label, LocalDate.of(year, 9, 1), LocalDate.of(year + 1, 6, 30));
    var response = post(API + "/academic-years", adminToken, request, AcademicYear.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  protected AcademicYear createAcademicYear(String adminToken) {
    return createAcademicYear(adminToken, "AY-" + UUID.randomUUID().toString().substring(0, 8));
  }

  protected Promotion createPromotion(String adminToken) {
    int gradYear = nextYear();
    var request = new PromotionCreateRequest("Promotion " + gradYear, gradYear);
    var response = post(API + "/promotions", adminToken, request, Promotion.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  protected StudentGroup createGroup(String adminToken) {
    var reference = "GRP-" + UUID.randomUUID().toString().substring(0, 8);
    var request = new StudentGroupCreateRequest(reference, "Group " + reference);
    var response = post(API + "/groups", adminToken, request, StudentGroup.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  protected void linkGroupToPromotion(String adminToken, UUID promotionId, UUID groupId) {
    var response =
        post(
            API + "/promotions/" + promotionId + "/groups/" + groupId,
            adminToken,
            null,
            Void.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  protected Course createCourse(String adminToken) {
    var reference = "CRS-" + UUID.randomUUID().toString().substring(0, 8);
    var request = new CourseCreateRequest(reference, "Course " + reference, 6);
    var response = post(API + "/courses", adminToken, request, Course.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  protected CourseAssignment createCourseAssignment(
      String adminToken, UUID groupId, UUID courseId, UUID academicYearId, int semester) {
    var request = new CourseAssignmentCreateRequest(groupId, courseId, academicYearId, semester);
    var response = post(API + "/course-assignments", adminToken, request, CourseAssignment.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }

  protected record BasePromotionFixture(
      Promotion promotion, StudentGroup group, AcademicYear academicYear) {}

  protected BasePromotionFixture createLinkedPromotionAndGroup(String adminToken) {
    var promotion = createPromotion(adminToken);
    var group = createGroup(adminToken);
    var academicYear = createAcademicYear(adminToken);
    linkGroupToPromotion(adminToken, promotion.getId(), group.getId());
    return new BasePromotionFixture(promotion, group, academicYear);
  }

  protected Student promoteToStudent(
      String adminToken,
      UUID userId,
      Promotion promotion,
      StudentGroup group,
      AcademicYear academicYear) {
    return promoteToStudent(adminToken, userId, promotion, group, academicYear, ProgramCode.EL);
  }

  /**
   * Promotes the user to student, then sets the program via PATCH
   * /students/{id}/program. A freshly-promoted student has no program (it is
   * null) until this second call, mirroring production behaviour. Pass {@code
   * null} to leave the program unset.
   */
  protected Student promoteToStudent(
      String adminToken,
      UUID userId,
      Promotion promotion,
      StudentGroup group,
      AcademicYear academicYear,
      ProgramCode programCode) {
    var request =
        new PromoteStudentRequest(
            "Jean",
            "Rakoto",
            promotion.getId(),
            group.getId(),
            academicYear.getId(),
            1,
            academicYear.getStartDate());
    var response =
        patch(API + "/users/" + userId + "/role/student", adminToken, request, UserAccount.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    var student =
        studentRepository
            .findByUserAccountIdWithAssociations(userId)
            .map(StudentMapper::toModel)
            .orElseThrow();

    if (programCode == null) {
      return student;
    }

    var programResponse =
        patch(
            API + "/students/" + student.getId() + "/program",
            adminToken,
            new StudentProgramChangeRequest(programCode),
            Student.class);
    assertThat(programResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    return programResponse.getBody();
  }

  protected UserAccount promoteToTeacher(String adminToken, UUID userId) {
    var request = new PromoteTeacherRequest("Marie", "Rasoa");
    var response =
        patch(API + "/users/" + userId + "/role/teacher", adminToken, request, UserAccount.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }
}
