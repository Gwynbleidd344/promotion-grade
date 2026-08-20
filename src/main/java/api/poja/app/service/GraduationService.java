package api.poja.app.service;

import api.poja.app.entity.Grade;
import api.poja.app.entity.Student;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mapper.GraduationListMapper;
import api.poja.app.model.GraduationListEntry;
import api.poja.app.model.GraduationStatus;
import api.poja.app.model.GraduationStatus.FailedCourse;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GraduationListEntryRepository;
import api.poja.app.repository.GraduationListRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class GraduationService {

  private static final BigDecimal PASSING_GRADE = BigDecimal.TEN;
  private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

  private final StudentRepository studentRepository;
  private final PromotionRepository promotionRepository;
  private final GradeRepository gradeRepository;
  private final ProgramCourseRepository programCourseRepository;
  private final GraduationListRepository graduationListRepository;
  private final GraduationListEntryRepository graduationListEntryRepository;
  private final BucketComponent bucketComponent;

  @Transactional(readOnly = true)
  public GraduationStatus getGraduationStatus(UUID studentId) {
    var student = findStudentOrThrow(studentId);
    if (student.getProgram() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Student has no program assigned");
    }
    var allGrades = gradeRepository.findByStudentId(studentId);

    var programCourses = programCourseRepository.findByProgramId(student.getProgram().getId());
    var courseIds =
        programCourses.stream().map(pc -> pc.getCourse().getId()).collect(Collectors.toSet());

    Map<UUID, List<Grade>> gradesByCourse =
        allGrades.stream()
            .filter(g -> g.getExam() != null && g.getExam().getGroupCourse() != null)
            .filter(g -> courseIds.contains(g.getExam().getGroupCourse().getCourse().getId()))
            .collect(Collectors.groupingBy(g -> g.getExam().getGroupCourse().getCourse().getId()));

    int totalCredits = 0;
    int creditsObtained = 0;
    BigDecimal creditWeightedSum = BigDecimal.ZERO;
    List<FailedCourse> failedCourses = new ArrayList<>();

    for (var pc : programCourses) {
      var course = pc.getCourse();
      var grades = gradesByCourse.getOrDefault(course.getId(), List.of());
      var validGrades = grades.stream().filter(g -> g.getValue() != null).toList();

      if (validGrades.isEmpty()) {
        failedCourses.add(
            FailedCourse.builder()
                .courseId(course.getId())
                .courseReference(course.getReference())
                .average(null)
                .build());
        continue;
      }

      BigDecimal valueSum = BigDecimal.ZERO;
      BigDecimal coefficientSum = BigDecimal.ZERO;
      for (var grade : validGrades) {
        var coefficient =
            (grade.getExam() != null && grade.getExam().getCoefficient() != null)
                ? grade.getExam().getCoefficient()
                : BigDecimal.ONE;
        valueSum = valueSum.add(grade.getValue().multiply(coefficient));
        coefficientSum = coefficientSum.add(coefficient);
      }

      var courseAverage =
          coefficientSum.signum() == 0
              ? BigDecimal.ZERO
              : valueSum.divide(coefficientSum, 4, RoundingMode.HALF_UP);

      creditWeightedSum =
          creditWeightedSum.add(courseAverage.multiply(BigDecimal.valueOf(course.getCredits())));
      totalCredits += course.getCredits();

      boolean passed = courseAverage.compareTo(PASSING_GRADE) >= 0;
      if (passed) {
        creditsObtained += course.getCredits();
      } else {
        failedCourses.add(
            FailedCourse.builder()
                .courseId(course.getId())
                .courseReference(course.getReference())
                .average(courseAverage)
                .build());
      }
    }

    BigDecimal generalAverage =
        totalCredits == 0
            ? null
            : creditWeightedSum.divide(BigDecimal.valueOf(totalCredits), 2, RoundingMode.HALF_UP);

    boolean graduated = failedCourses.isEmpty() && totalCredits > 0;

    return GraduationStatus.builder()
        .studentId(studentId)
        .graduated(graduated)
        .generalAverage(generalAverage)
        .totalCreditsObtained(creditsObtained)
        .failedCourses(failedCourses)
        .build();
  }

  @Transactional(readOnly = true)
  public List<GraduationListEntry> getGraduates(UUID promotionId, ProgramCode programCode) {
    findPromotionOrThrow(promotionId);
    List<Student> students;

    if (programCode != null) {
      students =
          studentRepository.findAll().stream()
              .filter(s -> s.getPromotion().getId().equals(promotionId))
              .filter(s -> s.getProgram() != null && s.getProgram().getCode() == programCode)
              .toList();
    } else {
      students = studentRepository.findByPromotionId(promotionId);
    }

    var graduatedStudents = new ArrayList<GraduationStatus>();
    for (var student : students) {
      var status = getGraduationStatus(student.getId());
      if (status.isGraduated()) {
        graduatedStudents.add(status);
      }
    }

    graduatedStudents.sort(
        Comparator.comparing(
            GraduationStatus::getGeneralAverage, Comparator.nullsLast(Comparator.reverseOrder())));

    var entries = new ArrayList<GraduationListEntry>();
    int rank = 1;
    for (var status : graduatedStudents) {
      var student = findStudentOrThrow(status.getStudentId());
      entries.add(
          GraduationListEntry.builder()
              .rank(rank++)
              .studentNumber(student.getStudentNumber())
              .lastName(student.getLastName())
              .firstName(student.getFirstName())
              .generalAverage(status.getGeneralAverage())
              .build());
    }
    return entries;
  }

  @SneakyThrows
  @Transactional
  public api.poja.app.model.GraduationListExport exportGraduates(UUID promotionId) {
    var promotion = findPromotionOrThrow(promotionId);
    var entries = getGraduates(promotionId, null);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("Diplômés");

    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("Rang");
    headerRow.createCell(1).setCellValue("Numéro étudiant");
    headerRow.createCell(2).setCellValue("Nom");
    headerRow.createCell(3).setCellValue("Prénom");
    headerRow.createCell(4).setCellValue("Moyenne générale");

    int rowNum = 1;
    for (var entry : entries) {
      Row row = sheet.createRow(rowNum++);
      row.createCell(0).setCellValue(entry.getRank());
      row.createCell(1).setCellValue(entry.getStudentNumber());
      row.createCell(2).setCellValue(entry.getLastName());
      row.createCell(3).setCellValue(entry.getFirstName());
      row.createCell(4)
          .setCellValue(
              entry.getGeneralAverage() != null ? entry.getGeneralAverage().doubleValue() : 0.0);
    }

    for (int i = 0; i < 5; i++) {
      sheet.autoSizeColumn(i);
    }

    File file = File.createTempFile("graduates-" + promotionId, ".xlsx");
    try (FileOutputStream fos = new FileOutputStream(file)) {
      workbook.write(fos);
    }
    workbook.close();

    var s3Key = "graduation/" + promotionId + "/" + LocalDateTime.now().toString() + ".xlsx";
    bucketComponent.upload(file, s3Key);

    var graduationList = GraduationListMapper.toNewEntity(promotion, s3Key);
    graduationList = graduationListRepository.save(graduationList);

    for (var entry : entries) {
      var student = findStudentByNumber(entry.getStudentNumber());
      var entryEntity =
          GraduationListMapper.toEntryEntity(
              graduationList, student, entry.getRank(), entry.getGeneralAverage());
      graduationListEntryRepository.save(entryEntity);
    }

    var downloadUrl = bucketComponent.presign(s3Key, DOWNLOAD_URL_TTL);

    return api.poja.app.model.GraduationListExport.builder()
        .promotionId(promotionId)
        .s3Key(s3Key)
        .downloadUrl(downloadUrl.toString())
        .generatedAt(LocalDateTime.now())
        .build();
  }

  private Student findStudentOrThrow(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
  }

  private Student findStudentByNumber(String studentNumber) {
    return studentRepository
        .findByStudentNumber(studentNumber)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found: " + studentNumber));
  }

  private api.poja.app.entity.Promotion findPromotionOrThrow(UUID id) {
    return promotionRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found"));
  }
}
