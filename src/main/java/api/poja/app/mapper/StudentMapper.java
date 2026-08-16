package api.poja.app.mapper;

import api.poja.app.entity.Program;
import api.poja.app.entity.Promotion;
import api.poja.app.entity.UserAccount;
import api.poja.app.model.Student;

public final class StudentMapper {

    private StudentMapper() {}

    public static Student toModel(api.poja.app.entity.Student entity) {
        if (entity == null) {
            return null;
        }
        return Student.builder()
                .id(entity.getId())
                .studentNumber(entity.getStudentNumber())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .program(entity.getProgram().getCode())
                .promotionId(entity.getPromotion().getId())
                .userAccountId(entity.getUserAccount().getId())
                .build();
    }

    public static api.poja.app.entity.Student toNewEntity(
            Student model, UserAccount userAccount, Program program, Promotion promotion) {
        var entity = new api.poja.app.entity.Student();
        entity.setUserAccount(userAccount);
        entity.setStudentNumber(model.getStudentNumber());
        entity.setFirstName(model.getFirstName());
        entity.setLastName(model.getLastName());
        entity.setProgram(program);
        entity.setPromotion(promotion);
        return entity;
    }
}