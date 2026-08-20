package api.poja.app.service;

import api.poja.app.endpoint.event.EventProducer;
import api.poja.app.endpoint.event.model.TranscriptSendRequested;
import api.poja.app.entity.Course;
import api.poja.app.entity.Grade;
import api.poja.app.entity.GroupCourse;
import api.poja.app.entity.ProgramCourse;
import api.poja.app.entity.enums.ReportStatus;
import api.poja.app.file.bucket.BucketComponent;
import api.poja.app.mapper.AcademicReportMapper;
import api.poja.app.model.AcademicReport;
import api.poja.app.repository.AcademicReportRepository;
import api.poja.app.repository.AcademicYearRepository;
import api.poja.app.repository.GradeRepository;
import api.poja.app.repository.GroupCourseRepository;
import api.poja.app.repository.ProgramCourseRepository;
import api.poja.app.repository.StudentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class AcademicReportService {

  private static final BigDecimal PASSING_GRADE = BigDecimal.TEN;
  private static final Duration DOWNLOAD_URL_TTL = Duration.ofMinutes(15);

  private final AcademicReportRepository academicReportRepository;
  private final StudentRepository studentRepository;
  private final AcademicYearRepository academicYearRepository;
  private final GradeRepository gradeRepository;
  private final GroupCourseRepository groupCourseRepository;
  private final ProgramCourseRepository programCourseRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer<TranscriptSendRequested> eventProducer;

  @Transactional(readOnly = true)
  public List<AcademicReport> list(UUID studentId) {
    findStudentOrThrow(studentId);
    return academicReportRepository.findByStudentId(studentId).stream()
        .map(this::toModelWithDownloadUrl)
        .toList();
  }

  @Transactional
  public AcademicReport generate(UUID studentId, String yearLabel) {
    var student = findStudentOrThrow(studentId);
    var academicYear = findAcademicYearOrThrow(yearLabel);

    var groupCoursesForYear =
        groupCourseRepository.findAll().stream()
            .filter(
                gc ->
                    gc.getAcademicYear() != null
                        && isSameAcademicYear(gc.getAcademicYear(), academicYear, yearLabel))
            .toList();

    List<Course> coursesToEvaluate;

    if (!groupCoursesForYear.isEmpty()) {
      coursesToEvaluate =
          groupCoursesForYear.stream()
              .map(GroupCourse::getCourse)
              .filter(Objects::nonNull)
              .distinct()
              .toList();
    } else if (student.getProgram() != null) {
      coursesToEvaluate =
          programCourseRepository.findByProgramId(student.getProgram().getId()).stream()
              .map(ProgramCourse::getCourse)
              .filter(Objects::nonNull)
              .distinct()
              .toList();
    } else {
      coursesToEvaluate = List.of();
    }

    Map<UUID, List<Grade>> gradesByCourse =
        gradeRepository.findByStudentId(studentId).stream()
            .filter(g -> g.getExam() != null && g.getExam().getGroupCourse() != null)
            .filter(
                g -> {
                  var ay = g.getExam().getGroupCourse().getAcademicYear();
                  return ay != null && isSameAcademicYear(ay, academicYear, yearLabel);
                })
            .collect(Collectors.groupingBy(g -> g.getExam().getGroupCourse().getCourse().getId()));

    int creditsConsidered = 0;
    int creditsObtained = 0;
    BigDecimal creditWeightedSum = BigDecimal.ZERO;
    boolean allCoursesGraded = !coursesToEvaluate.isEmpty();

    var reportLines = new ArrayList<TranscriptLine>();

    for (var course : coursesToEvaluate) {
      var grades = gradesByCourse.getOrDefault(course.getId(), List.of());
      var validGrades = grades.stream().filter(g -> g.getValue() != null).toList();

      if (validGrades.isEmpty()) {
        allCoursesGraded = false;
        reportLines.add(new TranscriptLine(course.getReference(), course.getTitle(), null, false));
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
      var passed = courseAverage.compareTo(PASSING_GRADE) >= 0;

      creditWeightedSum =
          creditWeightedSum.add(courseAverage.multiply(BigDecimal.valueOf(course.getCredits())));
      creditsConsidered += course.getCredits();
      if (passed) {
        creditsObtained += course.getCredits();
      }
      reportLines.add(
          new TranscriptLine(course.getReference(), course.getTitle(), courseAverage, passed));
    }

    BigDecimal generalAverage =
        creditsConsidered == 0
            ? null
            : creditWeightedSum.divide(
                BigDecimal.valueOf(creditsConsidered), 2, RoundingMode.HALF_UP);

    var status = allCoursesGraded ? ReportStatus.COMPLETE : ReportStatus.PROVISIONAL;

    var pdfFile =
        generateTranscriptPdf(
            student.getFirstName() + " " + student.getLastName(),
            student.getStudentNumber(),
            yearLabel,
            reportLines,
            generalAverage,
            creditsObtained,
            status);

    var bucketKey = "transcripts/" + studentId + "/" + yearLabel + ".pdf";
    bucketComponent.upload(pdfFile, bucketKey);

    var entity =
        academicReportRepository
            .findByStudentIdAndAcademicYearId(studentId, academicYear.getId())
            .orElseGet(api.poja.app.entity.AcademicReport::new);
    entity.setStudent(student);
    entity.setAcademicYear(academicYear);
    entity.setStatus(status);
    entity.setPdfS3Key(bucketKey);
    entity.setGeneratedAt(LocalDateTime.now());
    entity.setGeneralAverage(generalAverage);
    entity.setTotalCreditsObtained(creditsObtained);

    var saved = academicReportRepository.save(entity);
    return toModelWithDownloadUrl(saved);
  }

  @Transactional
  public void requestSend(UUID studentId, String yearLabel) {
    var student = findStudentOrThrow(studentId);
    var reportId = generate(studentId, yearLabel).getId();

    eventProducer.accept(
        List.of(
            TranscriptSendRequested.builder()
                .academicReportId(reportId)
                .userEmail(student.getUserAccount().getEmail())
                .build()));
  }

  private boolean isSameAcademicYear(
      api.poja.app.entity.AcademicYear targetYear,
      api.poja.app.entity.AcademicYear expectedYear,
      String yearLabel) {
    if (targetYear == null) return false;
    if (expectedYear != null
        && targetYear.getId() != null
        && targetYear.getId().equals(expectedYear.getId())) {
      return true;
    }
    return targetYear.getLabel() != null
        && yearLabel != null
        && targetYear.getLabel().trim().equalsIgnoreCase(yearLabel.trim());
  }

  private AcademicReport toModelWithDownloadUrl(api.poja.app.entity.AcademicReport entity) {
    var model = AcademicReportMapper.toModel(entity);
    if (entity.getPdfS3Key() != null) {
      model.setDownloadUrl(
          bucketComponent.presign(entity.getPdfS3Key(), DOWNLOAD_URL_TTL).toString());
    }
    return model;
  }

  private api.poja.app.entity.Student findStudentOrThrow(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
  }

  private api.poja.app.entity.AcademicYear findAcademicYearOrThrow(String label) {
    return academicYearRepository
        .findByLabel(label)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Unknown academic year: " + label));
  }

  @SneakyThrows
  private File generateTranscriptPdf(
      String studentFullName,
      String studentNumber,
      String yearLabel,
      List<TranscriptLine> lines,
      BigDecimal generalAverage,
      int creditsObtained,
      ReportStatus status) {
    var file = File.createTempFile("transcript-", ".pdf");
    var document = new Document();
    try (var out = new FileOutputStream(file)) {
      PdfWriter.getInstance(document, out);
      document.open();

      document.add(new Paragraph("Relevé de notes"));
      document.add(new Paragraph("Étudiant : " + studentFullName + " (" + studentNumber + ")"));
      document.add(new Paragraph("Année académique : " + yearLabel));
      document.add(
          new Paragraph(
              "Statut : " + (status == ReportStatus.COMPLETE ? "Définitif" : "Provisoire")));
      document.add(new Paragraph(" "));

      for (var line : lines) {
        var averageText =
            line.average() == null
                ? "-"
                : line.average().setScale(2, RoundingMode.HALF_UP).toString();
        var validatedText = line.average() == null ? "-" : (line.passed() ? "Oui" : "Non");
        document.add(
            new Paragraph(
                line.reference()
                    + " - "
                    + line.title()
                    + " : "
                    + averageText
                    + "/20 (validé : "
                    + validatedText
                    + ")"));
      }

      document.add(new Paragraph(" "));
      document.add(
          new Paragraph(
              "Moyenne générale : " + (generalAverage == null ? "-" : generalAverage + "/20")));
      document.add(new Paragraph("Crédits obtenus : " + creditsObtained));
      document.close();
    }
    return file;
  }

  private record TranscriptLine(
      String reference, String title, BigDecimal average, boolean passed) {}
}
