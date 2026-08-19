package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.entity.Exam;
import api.poja.app.entity.Grade;
import api.poja.app.entity.ProgramCourse;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.model.GraduationListEntry;
import api.poja.app.model.GraduationListExport;
import api.poja.app.model.GraduationStatus;
import api.poja.app.repository.CourseRepository;
import api.poja.app.repository.ExamRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.ProgramRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class GraduationControllerIT extends IntegrationTestSupport {

    @Autowired private CourseRepository courseRepository;
    @Autowired private GroupCourseRepository groupCourseRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private GradeRepository gradeRepository;
    @Autowired private ProgramRepository programRepository;
    @Autowired private ProgramCourseRepository programCourseRepository;

    private record StudentFixture(
            UUID studentId,
            String studentNumber,
            UUID promotionId,
            UUID groupId,
            UUID academicYearId,
            String yearLabel,
            UUID courseId,
            int courseCredits) {}

    @BeforeEach
    void setUp() {
        gradeRepository.deleteAll();
        examRepository.deleteAll();
        groupCourseRepository.deleteAll();
    }

    private StudentFixture createStudentWithGrades(String adminToken, boolean passed) {
        var base = createLinkedPromotionAndGroup(adminToken);
        var user = register("grad-student-" + UUID.randomUUID().toString().substring(0, 6));
        var student = promoteToStudent(
                adminToken, user.getId(), base.promotion(), base.group(), base.academicYear());

        var course = createCourse(adminToken);

        var program = programRepository.findByCode(ProgramCode.EL).orElseThrow();
        var courseEntity = courseRepository.findById(course.getId()).orElseThrow();
        var programCourse = new ProgramCourse();
        programCourse.setProgram(program);
        programCourse.setCourse(courseEntity);
        programCourseRepository.save(programCourse);

        var assignment = createCourseAssignment(
                adminToken, base.group().getId(), course.getId(), base.academicYear().getId(), 1);

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
        grade.setValue(passed ? new BigDecimal("15.00") : new BigDecimal("8.00"));
        grade.setCreatedAt(LocalDateTime.now());
        grade.setUpdatedAt(LocalDateTime.now());
        gradeRepository.save(grade);

        return new StudentFixture(
                student.getId(),
                student.getStudentNumber(),
                base.promotion().getId(),
                base.group().getId(),
                base.academicYear().getId(),
                base.academicYear().getLabel(),
                course.getId(),
                course.getCredits());
    }

    @Test
    void getGraduationStatus_returns_graduated_when_all_courses_passed() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);

        var response = get(API + "/students/" + fixture.studentId() + "/graduation", admin, GraduationStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isGraduated()).isTrue();
        assertThat(response.getBody().getStudentId()).isEqualTo(fixture.studentId());
        assertThat(response.getBody().getGeneralAverage()).isNotNull();
        assertThat(response.getBody().getTotalCreditsObtained()).isEqualTo(fixture.courseCredits());
        assertThat(response.getBody().getFailedCourses()).isEmpty();
    }

    @Test
    void getGraduationStatus_returns_not_graduated_when_course_failed() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, false);

        var response = get(API + "/students/" + fixture.studentId() + "/graduation", admin, GraduationStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isGraduated()).isFalse();
        assertThat(response.getBody().getFailedCourses()).isNotEmpty();
        assertThat(response.getBody().getFailedCourses().get(0).getAverage()).isLessThan(BigDecimal.TEN);
    }

    @Test
    void getGraduationStatus_returns_not_found_for_unknown_student() {
        var admin = bootstrapAdminToken();

        var response = get(API + "/students/" + UUID.randomUUID() + "/graduation", admin, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getGraduationStatus_requires_authentication() {
        var response = get(API + "/students/" + UUID.randomUUID() + "/graduation", null, ErrorResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void student_can_view_own_graduation_status() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);
        var user = registerAndLogin("self-grad-" + UUID.randomUUID().toString().substring(0, 6));

        var response = get(API + "/students/" + fixture.studentId() + "/graduation", user.token(), GraduationStatus.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getGraduates_returns_list_of_graduated_students_sorted_by_rank() {
        var admin = bootstrapAdminToken();

        var fixture1 = createStudentWithGrades(admin, true);

        var base2 = createLinkedPromotionAndGroup(admin);
        var user2 = register("grad-student2-" + UUID.randomUUID().toString().substring(0, 6));
        var student2 = promoteToStudent(
                admin, user2.getId(), base2.promotion(), base2.group(), base2.academicYear());
        var course2 = createCourse(admin);
        var program2 = programRepository.findByCode(ProgramCode.EL).orElseThrow();
        var courseEntity2 = courseRepository.findById(course2.getId()).orElseThrow();
        var programCourse2 = new ProgramCourse();
        programCourse2.setProgram(program2);
        programCourse2.setCourse(courseEntity2);
        programCourseRepository.save(programCourse2);
        var assignment2 = createCourseAssignment(
                admin, base2.group().getId(), course2.getId(), base2.academicYear().getId(), 1);
        var groupCourse2 = groupCourseRepository.findById(assignment2.getId()).orElseThrow();
        var exam2 = new Exam();
        exam2.setGroupCourse(groupCourse2);
        exam2.setName("Examen final");
        exam2.setExamDate(LocalDate.now());
        exam2.setExamTime(LocalTime.of(8, 0));
        exam2.setCoefficient(new BigDecimal("1.0000"));
        exam2 = examRepository.save(exam2);
        var studentEntity2 = studentRepository.findById(student2.getId()).orElseThrow();
        var grade2 = new Grade();
        grade2.setStudent(studentEntity2);
        grade2.setExam(exam2);
        grade2.setValue(new BigDecimal("18.00"));
        grade2.setCreatedAt(LocalDateTime.now());
        grade2.setUpdatedAt(LocalDateTime.now());
        gradeRepository.save(grade2);

        var response = restTemplate.exchange(
                API + "/promotions/" + fixture1.promotionId() + "/graduates?programCode=EL",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(admin)),
                new ParameterizedTypeReference<List<GraduationListEntry>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        var entries = response.getBody();
        for (int i = 0; i < entries.size(); i++) {
            assertThat(entries.get(i).getRank()).isEqualTo(i + 1);
        }
    }

    @Test
    void getGraduates_filters_by_program_code() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);

        var responseEL = restTemplate.exchange(
                API + "/promotions/" + fixture.promotionId() + "/graduates?programCode=EL",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(admin)),
                new ParameterizedTypeReference<List<GraduationListEntry>>() {});

        assertThat(responseEL.getStatusCode()).isEqualTo(HttpStatus.OK);

        var responseTN = restTemplate.exchange(
                API + "/promotions/" + fixture.promotionId() + "/graduates?programCode=TN",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(admin)),
                new ParameterizedTypeReference<List<GraduationListEntry>>() {});

        assertThat(responseTN.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseTN.getBody()).isEmpty();
    }

    @Test
    void getGraduates_returns_empty_list_when_no_graduates() {
        var admin = bootstrapAdminToken();
        var promotion = createPromotion(admin);

        var response = restTemplate.exchange(
                API + "/promotions/" + promotion.getId() + "/graduates",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(admin)),
                new ParameterizedTypeReference<List<GraduationListEntry>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getGraduates_returns_not_found_for_unknown_promotion() {
        var admin = bootstrapAdminToken();

        var response = get(API + "/promotions/" + UUID.randomUUID() + "/graduates", admin, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getGraduates_requires_admin_role() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);
        var user = registerAndLogin("nonadmin-grad-" + UUID.randomUUID().toString().substring(0, 6));

        var response = get(API + "/promotions/" + fixture.promotionId() + "/graduates", user.token(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void exportGraduates_generates_xlsx_and_returns_download_url() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);

        var response = get(API + "/promotions/" + fixture.promotionId() + "/graduates/export", admin, GraduationListExport.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var export = response.getBody();
        assertThat(export.getPromotionId()).isEqualTo(fixture.promotionId());
        assertThat(export.getS3Key()).isNotNull().isNotEmpty();
        assertThat(export.getDownloadUrl()).isNotNull().isNotEmpty();
        assertThat(export.getGeneratedAt()).isNotNull();
        assertThat(export.getDownloadUrl()).startsWith("https://");
    }

    @Test
    void exportGraduates_works_with_no_graduates() {
        var admin = bootstrapAdminToken();
        var promotion = createPromotion(admin);

        var response = get(API + "/promotions/" + promotion.getId() + "/graduates/export", admin, GraduationListExport.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var export = response.getBody();
        assertThat(export.getPromotionId()).isEqualTo(promotion.getId());
        assertThat(export.getS3Key()).isNotNull();
        assertThat(export.getDownloadUrl()).isNotNull();
    }

    @Test
    void exportGraduates_returns_not_found_for_unknown_promotion() {
        var admin = bootstrapAdminToken();

        var response = get(API + "/promotions/" + UUID.randomUUID() + "/graduates/export", admin, ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void exportGraduates_requires_admin_role() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);
        var user = registerAndLogin("nonadmin-export-" + UUID.randomUUID().toString().substring(0, 6));

        var response = get(API + "/promotions/" + fixture.promotionId() + "/graduates/export", user.token(), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void exportGraduates_produces_consistent_format() {
        var admin = bootstrapAdminToken();
        var fixture = createStudentWithGrades(admin, true);

        var response1 = get(API + "/promotions/" + fixture.promotionId() + "/graduates/export", admin, GraduationListExport.class);
        var response2 = get(API + "/promotions/" + fixture.promotionId() + "/graduates/export", admin, GraduationListExport.class);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody().getS3Key()).isNotEqualTo(response2.getBody().getS3Key());
    }
}