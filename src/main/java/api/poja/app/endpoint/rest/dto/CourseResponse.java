package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.Course;
import java.util.UUID;

public record CourseResponse(UUID id, String reference, String title, Integer credits) {

  public static CourseResponse from(Course course) {
    return new CourseResponse(
        course.getId(), course.getReference(), course.getTitle(), course.getCredits());
  }
}
