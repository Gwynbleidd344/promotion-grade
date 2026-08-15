package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.TeacherCreateRequest;
import api.poja.app.endpoint.rest.dto.TeacherResponse;
import api.poja.app.entity.Teacher;
import api.poja.app.entity.UserAccount;
import api.poja.app.entity.enums.UserRole;
import api.poja.app.repository.TeacherRepository;
import api.poja.app.repository.UserAccountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class TeacherService {

  private static final String EMPLOYEE_NUMBER_PREFIX = "TEC";

  private final TeacherRepository teacherRepository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final SequentialCodeGenerator sequentialCodeGenerator;

  public List<TeacherResponse> list(int page, int size) {
    return teacherRepository
        .findAll(PageRequest.of(page, size))
        .map(TeacherResponse::from)
        .getContent();
  }

  @Transactional
  public TeacherResponse create(TeacherCreateRequest request) {
    if (userAccountRepository.findByUsername(request.username()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
    if (userAccountRepository.findByEmail(request.email()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already taken");
    }

    var userAccount = new UserAccount();
    userAccount.setUsername(request.username());
    userAccount.setEmail(request.email());
    userAccount.setPasswordHash(passwordEncoder.encode(request.password()));
    userAccount.setRole(UserRole.TEC);
    userAccount.setEnabled(true);
    userAccount = userAccountRepository.save(userAccount);

    var teacher = new Teacher();
    teacher.setUserAccount(userAccount);
    teacher.setEmployeeNumber(nextEmployeeNumber());
    teacher.setFirstName(request.firstName());
    teacher.setLastName(request.lastName());

    return TeacherResponse.from(teacherRepository.save(teacher));
  }

  private String nextEmployeeNumber() {
    return sequentialCodeGenerator.generate(
        EMPLOYEE_NUMBER_PREFIX,
        teacherRepository.count(),
        candidate -> teacherRepository.findByEmployeeNumber(candidate).isPresent());
  }
}
