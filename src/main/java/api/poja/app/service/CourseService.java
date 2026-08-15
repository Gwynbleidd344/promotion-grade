package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.CourseCreateRequest;
import api.poja.app.mapper.CourseMapper;
import api.poja.app.model.Course;
import api.poja.app.repository.CourseRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CourseService {

  private final CourseRepository courseRepository;

  public List<Course> list(int page, int size) {
    return courseRepository
        .findAll(PageRequest.of(page, size))
        .map(CourseMapper::toModel)
        .getContent();
  }

  public Course create(CourseCreateRequest request) {
    if (courseRepository.findByReference(request.reference()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "A course with reference '" + request.reference() + "' already exists");
    }

    var toCreate =
        Course.builder()
            .reference(request.reference())
            .title(request.title())
            .credits(request.credits())
            .build();

    var saved = courseRepository.save(CourseMapper.toNewEntity(toCreate));
    return CourseMapper.toModel(saved);
  }
}
