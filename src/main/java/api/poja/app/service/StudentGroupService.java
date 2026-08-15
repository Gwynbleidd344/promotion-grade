package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.StudentGroupCreateRequest;
import api.poja.app.mapper.StudentGroupMapper;
import api.poja.app.model.StudentGroup;
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

  public List<StudentGroup> list(int page, int size) {
    return studentGroupRepository
        .findAll(PageRequest.of(page, size))
        .map(StudentGroupMapper::toModel)
        .getContent();
  }

  public StudentGroup create(StudentGroupCreateRequest request) {
    if (studentGroupRepository.findByReference(request.reference()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A group with reference '" + request.reference() + "' already exists");
    }

    var toCreate =
        StudentGroup.builder().reference(request.reference()).name(request.name()).build();

    var saved = studentGroupRepository.save(StudentGroupMapper.toNewEntity(toCreate));
    return StudentGroupMapper.toModel(saved);
  }
}
