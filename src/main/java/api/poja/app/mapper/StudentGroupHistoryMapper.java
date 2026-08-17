package api.poja.app.mapper;

import api.poja.app.model.StudentGroupHistory;

public final class StudentGroupHistoryMapper {

    private StudentGroupHistoryMapper() {}

    public static StudentGroupHistory toModel(api.poja.app.entity.StudentGroupHistory entity) {
        if (entity == null) {
            return null;
        }
        return StudentGroupHistory.builder()
                .id(entity.getId())
                .studentId(entity.getStudent().getId())
                .groupId(entity.getGroup().getId())
                .academicYearId(entity.getAcademicYear().getId())
                .semester(entity.getSemester())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .build();
    }
}