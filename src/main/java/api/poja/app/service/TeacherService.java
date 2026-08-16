package api.poja.app.service;

import api.poja.app.mapper.TeacherMapper;
import api.poja.app.model.Teacher;
import api.poja.app.repository.TeacherRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TeacherService {

  private final TeacherRepository teacherRepository;

  public List<Teacher> list(int page, int size) {
    return teacherRepository
        .findAll(PageRequest.of(page, size))
        .map(TeacherMapper::toModel)
        .getContent();
  }
}
