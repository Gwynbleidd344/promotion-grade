package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.entity.Exam;
import api.poja.app.entity.Grade;
import api.poja.app.entity.ProgramCourse;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.ReportStatus;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.model.AcademicReport;
import api.poja.app.repository.CourseRepository;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.ProgramRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class TranscriptControllerIT extends IntegrationTestSupport {

  @Autowired private CourseRepository courseRepository;
  @Autowired private GroupCourseRepository groupCourseRepository;
  @Autowired private ExamRepository examRepository;
  @Autowired private GradeRepository gradeRepository;
  @Autowired private ProgramRepository programRepository;
  @Autowired private ProgramCourseRepository programCourseRepository;

  // The real components talk to AWS (S3, EventBridge); stub them out so tests stay hermetic.
  @MockBean private BucketComponent bucketComponent;
  @MockBean private EventProducer<TranscriptSendRequested> eventProducer;

  @BeforeEach
  void stubBucket() throws Exception {
    when(bucketComponent.presign(any(), any()))
        .thenReturn(new URI("https://dummy-bucket.s3.amazonaws.com/signed").toURL());
  }

  private record StudentFixture(
      UUID studentId, UUID courseAssignmentId, String yearLabel, int courseCredits) {}

  private StudentFixture setUpStudent(String adminToken, boolean graded) {
    var base = createLinkedPromotionAndGroup(adminToken);
    var user = register("student-" + UUID.randomUUID());
    var student =
        promoteToStudent(
            adminToken, user.getId(), base.promotion(), base.group(), base.academicYear());

    var course = createCourse(adminToken);
    var program = programRepository.findByCode(ProgramCode.EL).orElseThrow();
    var courseEntity = courseRepository.findById(course.getId()).orElseThrow();
    var programCourse = new ProgramCourse();
    programCourse.setProgram(program);
    programCourse.setCourse(courseEntity);
    programCourseRepository.save(programCourse);

    var assignment =
        createCourseAssignment(
            adminToken, base.group().getId(), course.getId(), base.academicYear().getId(), 1);

    if (graded) {
      var groupCourse = groupCourseRepository.findById(assignment.getId()).orElseThrow();
      var exam = new Exam();
      exam.setGroupCourse(groupCourse);
      exam.setName("Examen final");
      exam.setExamDate(LocalDate.now());
      exam.setExamTime(LocalTime.of(8, 0));
      exam.setCoefficient(new BigDecimal("1.0000"));
      exam = examRepository.save(exam);

      var studentEntity = studentRepository.findById(student.getId()).orElseThrow();
      var grade = new Grade();
      grade.setStudent(studentEntity);
      grade.setExam(exam);
      grade.setValue(new BigDecimal("15.00"));
      grade.setCreatedAt(LocalDateTime.now());
      grade.setUpdatedAt(LocalDateTime.now());
      gradeRepository.save(grade);
    }

    return new StudentFixture(
        student.getId(), assignment.getId(), base.academicYear().getLabel(), course.getCredits());
  }

  private List<AcademicReport> listTranscripts(String token, UUID studentId) {
    var response =
        restTemplate.exchange(
            API + "/students/" + studentId + "/transcripts",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(token)),
            new ParameterizedTypeReference<List<AcademicReport>>() {});
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }

  @Test
  void list_transcripts_returns_empty_list_when_none_generated() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, false);

    assertThat(listTranscripts(admin, fixture.studentId())).isEmpty();
  }

  @Test
  void list_transcripts_for_unknown_student_returns_not_found() {
    var admin = bootstrapAdminToken();

    var response =
        get(API + "/students/" + UUID.randomUUID() + "/transcripts", admin, ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void generate_transcript_with_all_courses_graded_returns_complete_status() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, true);

    var response =
        post(
            API
                + "/students/"
                + fixture.studentId()
                + "/transcripts/"
                + fixture.yearLabel()
                + "/generate",
            admin,
            null,
            AcademicReport.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    var report = response.getBody();
    assertThat(report.getStatus()).isEqualTo(ReportStatus.COMPLETE);
    assertThat(report.getGeneralAverage()).isEqualByComparingTo("15.00");
    assertThat(report.getTotalCreditsObtained()).isEqualTo(fixture.courseCredits());
    assertThat(report.getPdfS3Key()).isNotBlank();
    assertThat(report.getDownloadUrl()).isNotBlank();
  }

  @Test
  void generate_transcript_with_ungraded_course_returns_provisional_status() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, false);

    var response =
        post(
            API
                + "/students/"
                + fixture.studentId()
                + "/transcripts/"
                + fixture.yearLabel()
                + "/generate",
            admin,
            null,
            AcademicReport.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getStatus()).isEqualTo(ReportStatus.PROVISIONAL);
    assertThat(response.getBody().getGeneralAverage()).isNull();
  }

  @Test
  void generate_transcript_for_unknown_student_returns_not_found() {
    var admin = bootstrapAdminToken();
    var academicYear = createAcademicYear(admin);

    var response =
        post(
            API
                + "/students/"
                + UUID.randomUUID()
                + "/transcripts/"
                + academicYear.getLabel()
                + "/generate",
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void generate_transcript_for_unknown_academic_year_returns_not_found() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, false);

    var response =
        post(
            API + "/students/" + fixture.studentId() + "/transcripts/UNKNOWN-YEAR/generate",
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void non_admin_cannot_generate_transcript() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, false);
    var teacherUser = register("teacher-" + UUID.randomUUID());
    promoteToTeacher(admin, teacherUser.getId());
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        post(
            API
                + "/students/"
                + fixture.studentId()
                + "/transcripts/"
                + fixture.yearLabel()
                + "/generate",
            teacherToken,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void list_transcripts_returns_previously_generated_report() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, true);
    post(
        API
            + "/students/"
            + fixture.studentId()
            + "/transcripts/"
            + fixture.yearLabel()
            + "/generate",
        admin,
        null,
        AcademicReport.class);

    var reports = listTranscripts(admin, fixture.studentId());

    assertThat(reports).hasSize(1);
    assertThat(reports.get(0).getStatus()).isEqualTo(ReportStatus.COMPLETE);
  }

  @Test
  void send_transcript_without_prior_generation_generates_one_and_dispatches_event() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, true);

    var response =
        post(
            API
                + "/students/"
                + fixture.studentId()
                + "/transcripts/"
                + fixture.yearLabel()
                + "/send",
            admin,
            null,
            Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    verify(eventProducer, times(1)).accept(any());

    var reports = listTranscripts(admin, fixture.studentId());
    assertThat(reports).hasSize(1);
    assertThat(reports.getFirst().getPdfS3Key()).isNotBlank();
  }

  @Test
  void send_transcript_reuses_already_generated_report_instead_of_duplicating_it() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, true);
    post(
        API
            + "/students/"
            + fixture.studentId()
            + "/transcripts/"
            + fixture.yearLabel()
            + "/generate",
        admin,
        null,
        AcademicReport.class);

    var response =
        post(
            API
                + "/students/"
                + fixture.studentId()
                + "/transcripts/"
                + fixture.yearLabel()
                + "/send",
            admin,
            null,
            Void.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    verify(eventProducer, times(1)).accept(any());
    assertThat(listTranscripts(admin, fixture.studentId())).hasSize(1);
  }

  @Test
  void send_transcript_on_unknown_academic_year_returns_not_found() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, false);

    var response =
        post(
            API + "/students/" + fixture.studentId() + "/transcripts/UNKNOWN-YEAR/send",
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void non_admin_cannot_send_transcript() {
    var admin = bootstrapAdminToken();
    var fixture = setUpStudent(admin, true);
    var teacherUser = register("teacher-" + UUID.randomUUID());
    promoteToTeacher(admin, teacherUser.getId());
    var teacherToken = login(teacherUser.getUsername(), DEFAULT_PASSWORD);

    var response =
        post(
            API
                + "/students/"
                + fixture.studentId()
                + "/transcripts/"
                + fixture.yearLabel()
                + "/send",
            teacherToken,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
