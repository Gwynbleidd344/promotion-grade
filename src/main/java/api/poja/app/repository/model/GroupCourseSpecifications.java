package api.poja.app.repository.model;

import api.poja.app.entity.GroupCourse;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class GroupCourseSpecifications {

  private GroupCourseSpecifications() {}

  public static Specification<GroupCourse> matching(
      UUID groupId, UUID courseId, UUID academicYearId, Integer semester) {
    return (root, query, cb) -> {
      var predicate = cb.conjunction();
      if (groupId != null) {
        predicate = cb.and(predicate, cb.equal(root.get("group").get("id"), groupId));
      }
      if (courseId != null) {
        predicate = cb.and(predicate, cb.equal(root.get("course").get("id"), courseId));
      }
      if (academicYearId != null) {
        predicate = cb.and(predicate, cb.equal(root.get("academicYear").get("id"), academicYearId));
      }
      if (semester != null) {
        predicate = cb.and(predicate, cb.equal(root.get("semester"), semester));
      }
      return predicate;
    };
  }
}
