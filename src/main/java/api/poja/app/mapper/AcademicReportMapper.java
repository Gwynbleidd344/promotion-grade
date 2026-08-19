package api.poja.app.mapper;

import api.poja.app.model.AcademicReport;

public final class AcademicReportMapper {

  private AcademicReportMapper() {}

  public static AcademicReport toModel(api.poja.app.entity.AcademicReport entity) {
    if (entity == null) {
      return null;
    }
    return AcademicReport.builder()
        .id(entity.getId())
        .studentId(entity.getStudent().getId())
        .academicYearId(entity.getAcademicYear().getId())
        .status(entity.getStatus())
        .pdfS3Key(entity.getPdfS3Key())
        .generatedAt(entity.getGeneratedAt())
        .sentAt(entity.getSentAt())
        .generalAverage(entity.getGeneralAverage())
        .totalCreditsObtained(entity.getTotalCreditsObtained())
        .build();
  }
}
