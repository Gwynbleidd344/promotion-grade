package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.StudentGroupCreateRequest;
import api.poja.app.endpoint.rest.dto.StudentGroupResponse;
import api.poja.app.entity.StudentGroup;
import api.poja.app.repository.StudentGroupRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class StudentGroupService {

  private final StudentGroupRepository studentGroupRepository;

  public List<StudentGroupResponse> list(int page, int size) {
    return studentGroupRepository
        .findAll(PageRequest.of(page, size))
        .map(StudentGroupResponse::from)
        .getContent();
  }

  public StudentGroupResponse create(StudentGroupCreateRequest request) {
    if (studentGroupRepository.findByReference(request.reference()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A group with reference '" + request.reference() + "' already exists");
    }

    var group = new StudentGroup();
    group.setReference(request.reference());
    group.setName(request.name());

    return StudentGroupResponse.from(studentGroupRepository.save(group));
  }
}
