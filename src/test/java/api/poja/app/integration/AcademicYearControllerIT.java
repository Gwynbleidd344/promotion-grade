package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.AcademicYearCreateRequest;
import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.model.AcademicYear;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class AcademicYearControllerIT extends IntegrationTestSupport {

  @Test
  void create_and_list_academic_years_sorted_by_start_date() {
    var admin = bootstrapAdminToken();
    String labelA = "SORT-A-" + uniqueUsername("y");
    String labelB = "SORT-B-" + uniqueUsername("y");

    int yearA = nextYear();
    int yearB = nextYear();

    var later = createAcademicYear(admin, labelB, yearB);
    var earlier = createAcademicYear(admin, labelA, yearA);

    var response =
        restTemplate.exchange(
            API + "/academic-years?page=0&size=500",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<AcademicYear>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var ids = response.getBody().stream().map(AcademicYear::getId).toList();
    assertThat(ids).contains(earlier.getId(), later.getId());
    assertThat(ids.indexOf(earlier.getId())).isLessThan(ids.indexOf(later.getId()));
  }

  @Test
  void create_rejects_duplicate_label() {
    var admin = bootstrapAdminToken();
    int year = nextYear();
    String label = "DUP-LABEL-" + year;

    createAcademicYear(admin, label, year);

    var response =
        post(
            API + "/academic-years",
            admin,
            new AcademicYearCreateRequest(
                label, LocalDate.of(year + 10, 9, 1), LocalDate.of(year + 11, 6, 30)),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already exists");
  }

  @Test
  void create_rejects_end_date_before_start_date() {
    var admin = bootstrapAdminToken();
    int year = nextYear();

    var response =
        post(
            API + "/academic-years",
            admin,
            new AcademicYearCreateRequest(
                "BADRANGE-" + uniqueUsername("y"),
                LocalDate.of(year, 6, 30),
                LocalDate.of(year - 1, 9, 1)),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("endDate must be strictly after startDate");
  }

  @Test
  void create_rejects_overlapping_date_range() {
    var admin = bootstrapAdminToken();
    int year = nextYear();
    String baseLabel = "BASE-" + year;

    createAcademicYear(admin, baseLabel, year);

    var response =
        post(
            API + "/academic-years",
            admin,
            new AcademicYearCreateRequest(
                "OVERLAP-" + year, LocalDate.of(year + 1, 1, 1), LocalDate.of(year + 1, 12, 31)),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("overlaps");
  }

  @Test
  void create_rejects_missing_required_fields() {
    var admin = bootstrapAdminToken();

    var response =
        post(
            API + "/academic-years",
            admin,
            new AcademicYearCreateRequest(" ", null, null),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void list_is_readable_without_admin_role() {
    var user = registerAndLogin("ayreader");

    var response = get(API + "/academic-years", user.token(), String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private AcademicYear createAcademicYear(String adminToken, String label, int year) {
    var request =
        new AcademicYearCreateRequest(
            label, LocalDate.of(year, 9, 1), LocalDate.of(year + 1, 6, 30));
    var response = post(API + "/academic-years", adminToken, request, AcademicYear.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return response.getBody();
  }
}
