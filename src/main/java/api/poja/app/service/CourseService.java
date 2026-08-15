package api.poja.app.service;

import api.poja.app.endpoint.rest.dto.CourseCreateRequest;
import api.poja.app.endpoint.rest.dto.CourseResponse;
import api.poja.app.entity.Course;
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

  public List<CourseResponse> list(int page, int size) {
    return courseRepository.findAll(PageRequest.of(page, size)).map(CourseResponse::from)
        .getContent();
  }

  public CourseResponse create(CourseCreateRequest request) {
    if (courseRepository.findByReference(request.reference()).isPresent()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A course with reference '" + request.reference() + "' already exists");
    }

    var course = new Course();
    course.setReference(request.reference());
    course.setTitle(request.title());
    course.setCredits(request.credits());

    return CourseResponse.from(courseRepository.save(course));
  }
}
