// FILE: src/main/java/api/poja/app/service/StudentService.java
package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.StudentCreateRequest;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.ProgramCode;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.mapper.StudentMapper;
import api.poja.app.model.Student;
import api.poja.app.repository.ProgramRepository;
import api.poja.app.repository.PromotionRepository;
import api.poja.app.repository.StudentRepository;
import api.poja.app.repository.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class StudentService {

    private static final String STUDENT_NUMBER_PREFIX = "STD";

    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final ProgramRepository programRepository;
    private final PromotionRepository promotionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SequentialCodeGenerator sequentialCodeGenerator;

    public List<Student> list(UUID promotionId, ProgramCode programCode, int page, int size) {
        return studentRepository
                .findAllFiltered(promotionId, programCode, PageRequest.of(page, size))
                .map(StudentMapper::toModel)
                .getContent();
    }

    public Student getById(UUID id) {
        return StudentMapper.toModel(findStudentOrThrow(id));
    }

    @Transactional
    public Student create(StudentCreateRequest request) {
        if (userAccountRepository.findByUsername(request.username()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (userAccountRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
        }

        var program =
                programRepository
                        .findByCode(request.program())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Unknown program: " + request.program()));
        var promotion =
                promotionRepository
                        .findById(request.promotionId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Unknown promotion: " + request.promotionId()));

        var userAccount = new UserAccount();
        userAccount.setUsername(request.username());
        userAccount.setEmail(request.email());
        userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
        userAccount.setRole(UserRole.STD);
        userAccount.setEnabled(true);
        userAccount = userAccountRepository.save(userAccount);

        var toCreate =
                Student.builder().firstName(request.firstName()).lastName(request.lastName()).build();

        var entity = StudentMapper.toNewEntity(toCreate, userAccount, program, promotion);
        entity.setStudentNumber(nextStudentNumber());

        return StudentMapper.toModel(studentRepository.save(entity));
    }

    private String nextStudentNumber() {
        return sequentialCodeGenerator.generate(
                STUDENT_NUMBER_PREFIX,
                studentRepository.count(),
                candidate -> studentRepository.findByStudentNumber(candidate).isPresent());
    }

    private api.poja.app.entity.Student findStudentOrThrow(UUID id) {
        return studentRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }
}