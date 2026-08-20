package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.entity.AcademicYear;
import api.poja.app.entity.Course;
import api.poja.app.entity.Exam;
import api.poja.app.entity.Grade;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.Program;
import api.poja.app.entity.ProgramCourse;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.Student;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.ReportStatus;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.repository.AcademicReportRepository;
import api.poja.app.repository.AcademicYearRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.StudentRepository;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicReportServiceTest {

  @Mock private AcademicReportRepository academicReportRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private AcademicYearRepository academicYearRepository;
  @Mock private GradeRepository gradeRepository;
  @Mock private GroupCourseRepository groupCourseRepository;
  @Mock private ProgramCourseRepository programCourseRepository;
  @Mock private BucketComponent bucketComponent;
  @Mock private EventProducer<TranscriptSendRequested> eventProducer;

  private AcademicReportService service;

  private UUID studentId;
  private UUID programId;
  private UUID academicYearId;

  @BeforeEach
  void setUp() {
    service =
        new AcademicReportService(
            academicReportRepository,
            studentRepository,
            academicYearRepository,
            gradeRepository,
            groupCourseRepository,
            programCourseRepository,
            bucketComponent,
            eventProducer);
    studentId = UUID.randomUUID();
    programId = UUID.randomUUID();
    academicYearId = UUID.randomUUID();
  }

  private Program program() {
    var p = new Program();
    p.setId(programId);
    p.setCode(ProgramCode.EL);
    return p;
  }

  private Student student() {
    var userAccount = new UserAccount();
    userAccount.setId(UUID.randomUUID());
    userAccount.setEmail("student@example.com");

    var s = new Student();
    s.setId(studentId);
    s.setUserAccount(userAccount);
    s.setStudentNumber("STD001");
    s.setFirstName("Jean");
    s.setLastName("Rakoto");
    s.setProgram(program());
    var promotion = new Promotion();
    promotion.setId(UUID.randomUUID());
    s.setPromotion(promotion);
    return s;
  }

  private AcademicYear academicYear() {
    var y = new AcademicYear();
    y.setId(academicYearId);
    y.setLabel("2025-2026");
    return y;
  }

  private Course course(String reference, int credits) {
    var c = new Course();
    c.setId(UUID.randomUUID());
    c.setReference(reference);
    c.setTitle("Cours " + reference);
    c.setCredits(credits);
    return c;
  }

  private Grade gradeFor(Course course, BigDecimal value, AcademicYear year) {
    var groupCourse = new GroupCourse();
    groupCourse.setCourse(course);
    groupCourse.setAcademicYear(year);

    var exam = new Exam();
    exam.setId(UUID.randomUUID());
    exam.setGroupCourse(groupCourse);
    exam.setCoefficient(BigDecimal.ONE);

    var grade = new Grade();
    grade.setId(UUID.randomUUID());
    grade.setExam(exam);
    grade.setValue(value);
    return grade;
  }

  @Test
  void generates_a_complete_report_when_all_year_courses_are_passed() throws Exception {
    var year = academicYear();
    var course = course("PROG1", 5);
    var groupCourseForYear = new GroupCourse();
    groupCourseForYear.setCourse(course);
    groupCourseForYear.setAcademicYear(year);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(academicYearRepository.findByLabel("2025-2026")).thenReturn(Optional.of(year));
    when(groupCourseRepository.findAll()).thenReturn(List.of(groupCourseForYear));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(gradeFor(course, new BigDecimal("14"), year)));
    when(academicReportRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
        .thenReturn(Optional.empty());
    when(academicReportRepository.save(any()))
        .thenAnswer(
            invocation -> {
              api.poja.app.entity.AcademicReport r = invocation.getArgument(0);
              r.setId(UUID.randomUUID());
              return r;
            });
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://bucket.example.com/report.pdf"));

    var result = service.generate(studentId, "2025-2026");

    assertThat(result.getStatus()).isEqualTo(ReportStatus.COMPLETE);
    assertThat(result.getGeneralAverage()).isEqualByComparingTo("14.00");
    verify(bucketComponent).upload(any(), anyString());
  }

  @Test
  void generates_a_provisional_report_when_a_course_has_no_grade() throws MalformedURLException {
    var year = academicYear();
    var course1 = course("PROG1", 5);
    var course2 = course("WEB1", 5);
    var gc1 = new GroupCourse();
    gc1.setCourse(course1);
    gc1.setAcademicYear(year);
    var gc2 = new GroupCourse();
    gc2.setCourse(course2);
    gc2.setAcademicYear(year);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(academicYearRepository.findByLabel("2025-2026")).thenReturn(Optional.of(year));
    when(groupCourseRepository.findAll()).thenReturn(List.of(gc1, gc2));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(gradeFor(course1, new BigDecimal("14"), year)));
    when(academicReportRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
        .thenReturn(Optional.empty());
    when(academicReportRepository.save(any()))
        .thenAnswer(
            invocation -> {
              api.poja.app.entity.AcademicReport r = invocation.getArgument(0);
              r.setId(UUID.randomUUID());
              return r;
            });
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://bucket.example.com/report.pdf"));

    var result = service.generate(studentId, "2025-2026");

    assertThat(result.getStatus()).isEqualTo(ReportStatus.PROVISIONAL);
  }

  @Test
  void falls_back_to_program_courses_when_no_group_course_exists_for_the_year()
      throws MalformedURLException {
    var year = academicYear();
    var course = course("PROG1", 5);
    var programCourse = new ProgramCourse();
    programCourse.setId(UUID.randomUUID());
    programCourse.setProgram(program());
    programCourse.setCourse(course);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(academicYearRepository.findByLabel("2025-2026")).thenReturn(Optional.of(year));
    when(groupCourseRepository.findAll()).thenReturn(List.of());
    when(programCourseRepository.findByProgramId(programId)).thenReturn(List.of(programCourse));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());
    when(academicReportRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
        .thenReturn(Optional.empty());
    when(academicReportRepository.save(any()))
        .thenAnswer(
            invocation -> {
              api.poja.app.entity.AcademicReport r = invocation.getArgument(0);
              r.setId(UUID.randomUUID());
              return r;
            });
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://bucket.example.com/report.pdf"));

    var result = service.generate(studentId, "2025-2026");

    assertThat(result.getStatus()).isEqualTo(ReportStatus.PROVISIONAL);
  }

  @Test
  void request_send_reuses_existing_report_when_pdf_already_generated() {
    var year = academicYear();
    var existingId = UUID.randomUUID();
    var existing = new api.poja.app.entity.AcademicReport();
    existing.setId(existingId);
    existing.setPdfS3Key("transcripts/x/2025-2026.pdf");

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(academicYearRepository.findByLabel("2025-2026")).thenReturn(Optional.of(year));
    when(academicReportRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
        .thenReturn(Optional.of(existing));

    service.requestSend(studentId, "2025-2026");

    verify(eventProducer, times(1)).accept(any());
    verify(academicReportRepository, never()).save(any());
  }

  @Test
  void request_send_generates_report_first_when_none_exists_yet() throws Exception {
    var year = academicYear();
    var course = course("PROG1", 5);
    var gc = new GroupCourse();
    gc.setCourse(course);
    gc.setAcademicYear(year);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student()));
    when(academicYearRepository.findByLabel("2025-2026")).thenReturn(Optional.of(year));
    when(academicReportRepository.findByStudentIdAndAcademicYearId(studentId, academicYearId))
        .thenReturn(Optional.empty());
    when(groupCourseRepository.findAll()).thenReturn(List.of(gc));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(gradeFor(course, new BigDecimal("14"), year)));
    when(academicReportRepository.save(any()))
        .thenAnswer(
            invocation -> {
              api.poja.app.entity.AcademicReport r = invocation.getArgument(0);
              r.setId(UUID.randomUUID());
              return r;
            });
    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://bucket.example.com/report.pdf"));

    service.requestSend(studentId, "2025-2026");

    verify(eventProducer, times(1)).accept(any());
    verify(bucketComponent).upload(any(), anyString());
  }
}
