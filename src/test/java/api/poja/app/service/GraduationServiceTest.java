package api.poja.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api.poja.app.entity.Course;
import api.poja.app.entity.Exam;
import api.poja.app.entity.Grade;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.Program;
import api.poja.app.entity.ProgramCourse;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.Student;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GraduationListEntryRepository;
import api.poja.app.repository.GraduationListRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentRepository;
import java.math.BigDecimal;
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
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GraduationServiceTest {

  @Mock private StudentRepository studentRepository;
  @Mock private PromotionRepository promotionRepository;
  @Mock private GradeRepository gradeRepository;
  @Mock private ProgramCourseRepository programCourseRepository;
  @Mock private GraduationListRepository graduationListRepository;
  @Mock private GraduationListEntryRepository graduationListEntryRepository;
  @Mock private BucketComponent bucketComponent;

  private GraduationService service;

  private UUID studentId;
  private UUID promotionId;
  private UUID programId;

  @BeforeEach
  void setUp() {
    service =
        new GraduationService(
            studentRepository,
            promotionRepository,
            gradeRepository,
            programCourseRepository,
            graduationListRepository,
            graduationListEntryRepository,
            bucketComponent);
    studentId = UUID.randomUUID();
    promotionId = UUID.randomUUID();
    programId = UUID.randomUUID();
  }

  private Program program() {
    var p = new Program();
    p.setId(programId);
    p.setCode(ProgramCode.EL);
    return p;
  }

  private Promotion promotion() {
    var p = new Promotion();
    p.setId(promotionId);
    p.setName("Promo 2026");
    p.setGraduationYear(2026);
    return p;
  }

  private Student student(String number) {
    var s = new Student();
    s.setId(studentId);
    s.setStudentNumber(number);
    s.setFirstName("Jean");
    s.setLastName("Rakoto");
    s.setProgram(program());
    s.setPromotion(promotion());
    return s;
  }

  private Course course(String reference, int credits) {
    var c = new Course();
    c.setId(UUID.randomUUID());
    c.setReference(reference);
    c.setTitle("Cours " + reference);
    c.setCredits(credits);
    return c;
  }

  private ProgramCourse programCourse(Course course) {
    var pc = new ProgramCourse();
    pc.setId(UUID.randomUUID());
    pc.setProgram(program());
    pc.setCourse(course);
    return pc;
  }

  private Grade gradeFor(Course course, BigDecimal value, BigDecimal coefficient) {
    var groupCourse = new GroupCourse();
    groupCourse.setCourse(course);

    var exam = new Exam();
    exam.setId(UUID.randomUUID());
    exam.setGroupCourse(groupCourse);
    exam.setCoefficient(coefficient);

    var grade = new Grade();
    grade.setId(UUID.randomUUID());
    grade.setExam(exam);
    grade.setValue(value);
    return grade;
  }

  @Test
  void graduation_status_is_true_when_all_courses_pass() {
    var student = student("STD001");
    var prog1 = course("PROG1", 5);
    var web1 = course("WEB1", 5);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(prog1), programCourse(web1)));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(
            List.of(
                gradeFor(prog1, new BigDecimal("15"), BigDecimal.ONE),
                gradeFor(web1, new BigDecimal("12"), BigDecimal.ONE)));

    var status = service.getGraduationStatus(studentId);

    assertThat(status.isGraduated()).isTrue();
    assertThat(status.getFailedCourses()).isEmpty();
    assertThat(status.getTotalCreditsObtained()).isEqualTo(10);
  }

  @Test
  void graduation_status_is_false_when_a_course_average_is_below_ten() {
    var student = student("STD002");
    var prog1 = course("PROG1", 5);
    var web1 = course("WEB1", 5);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(prog1), programCourse(web1)));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(
            List.of(
                gradeFor(prog1, new BigDecimal("15"), BigDecimal.ONE),
                gradeFor(web1, new BigDecimal("8"), BigDecimal.ONE)));

    var status = service.getGraduationStatus(studentId);

    assertThat(status.isGraduated()).isFalse();
    assertThat(status.getFailedCourses()).hasSize(1);
    assertThat(status.getFailedCourses().get(0).getCourseReference()).isEqualTo("WEB1");
  }

  @Test
  void graduation_status_is_false_when_a_course_has_no_grade_at_all() {
    var student = student("STD003");
    var prog1 = course("PROG1", 5);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(prog1)));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());

    var status = service.getGraduationStatus(studentId);

    assertThat(status.isGraduated()).isFalse();
    assertThat(status.getFailedCourses()).hasSize(1);
    assertThat(status.getFailedCourses().get(0).getAverage()).isNull();
  }

  @Test
  void graduation_status_weighs_exam_coefficients_within_a_course() {
    var student = student("STD004");
    var prog1 = course("PROG1", 5);

    when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(prog1)));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(
            List.of(
                gradeFor(prog1, new BigDecimal("20"), new BigDecimal("0.25")),
                gradeFor(prog1, new BigDecimal("8"), new BigDecimal("0.75"))));

    var status = service.getGraduationStatus(studentId);

    // (20*0.25 + 8*0.75) / (0.25+0.75) = 11
    assertThat(status.getGeneralAverage()).isEqualByComparingTo("11.00");
    assertThat(status.isGraduated()).isTrue();
  }

  @Test
  void graduates_list_is_sorted_by_general_average_descending_with_rank() {
    var student1 = UUID.randomUUID();
    var student2 = UUID.randomUUID();
    var promo = promotion();

    var s1 = new Student();
    s1.setId(student1);
    s1.setStudentNumber("STD001");
    s1.setFirstName("A");
    s1.setLastName("A");
    s1.setProgram(program());
    s1.setPromotion(promo);

    var s2 = new Student();
    s2.setId(student2);
    s2.setStudentNumber("STD002");
    s2.setFirstName("B");
    s2.setLastName("B");
    s2.setProgram(program());
    s2.setPromotion(promo);

    var course = course("PROG1", 5);

    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promo));
    when(studentRepository.findByPromotionId(promotionId)).thenReturn(List.of(s1, s2));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(course)));
    when(studentRepository.findById(student1)).thenReturn(Optional.of(s1));
    when(studentRepository.findById(student2)).thenReturn(Optional.of(s2));
    when(gradeRepository.findByStudentId(student1))
        .thenReturn(List.of(gradeFor(course, new BigDecimal("12"), BigDecimal.ONE)));
    when(gradeRepository.findByStudentId(student2))
        .thenReturn(List.of(gradeFor(course, new BigDecimal("18"), BigDecimal.ONE)));

    var result = service.getGraduates(promotionId, null);

    assertThat(result).extracting("studentNumber").containsExactly("STD002", "STD001");
    assertThat(result.get(0).getRank()).isEqualTo(1);
    assertThat(result.get(1).getRank()).isEqualTo(2);
  }

  @Test
  void graduates_list_excludes_non_graduated_students() {
    var promo = promotion();
    var s1 = new Student();
    s1.setId(studentId);
    s1.setStudentNumber("STD001");
    s1.setFirstName("A");
    s1.setLastName("A");
    s1.setProgram(program());
    s1.setPromotion(promo);

    var course = course("PROG1", 5);

    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promo));
    when(studentRepository.findByPromotionId(promotionId)).thenReturn(List.of(s1));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(course)));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(s1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(gradeFor(course, new BigDecimal("5"), BigDecimal.ONE)));

    var result = service.getGraduates(promotionId, null);

    assertThat(result).isEmpty();
  }

  @Test
  void rejects_graduates_listing_when_promotion_not_found() {
    when(promotionRepository.findById(promotionId)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        ResponseStatusException.class, () -> service.getGraduates(promotionId, null));
  }

  @Test
  void exports_graduates_to_xlsx_and_uploads_to_bucket() throws Exception {
    var promo = promotion();
    var s1 = new Student();
    s1.setId(studentId);
    s1.setStudentNumber("STD001");
    s1.setFirstName("Jean");
    s1.setLastName("Rakoto");
    s1.setProgram(program());
    s1.setPromotion(promo);

    var course = course("PROG1", 5);

    when(promotionRepository.findById(promotionId)).thenReturn(Optional.of(promo));
    when(studentRepository.findByPromotionId(promotionId)).thenReturn(List.of(s1));
    when(programCourseRepository.findByProgramId(programId))
        .thenReturn(List.of(programCourse(course)));
    when(studentRepository.findById(studentId)).thenReturn(Optional.of(s1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(gradeFor(course, new BigDecimal("15"), BigDecimal.ONE)));
    when(studentRepository.findByStudentNumber("STD001")).thenReturn(Optional.of(s1));

    when(bucketComponent.presign(anyString(), any(Duration.class)))
        .thenReturn(new URL("https://bucket.example.com/graduates.xlsx"));

    var savedList = new api.poja.app.entity.GraduationList();
    savedList.setId(UUID.randomUUID());
    when(graduationListRepository.save(any())).thenReturn(savedList);

    var result = service.exportGraduates(promotionId);

    assertThat(result.getPromotionId()).isEqualTo(promotionId);
    assertThat(result.getDownloadUrl()).isEqualTo("https://bucket.example.com/graduates.xlsx");
    verify(bucketComponent).upload(any(), anyString());
    verify(graduationListEntryRepository).save(any());
  }
}
