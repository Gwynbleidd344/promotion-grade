package api.poja.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import api.poja.app.endpoint.rest.dto.ErrorResponse;
import api.poja.app.endpoint.rest.dto.PromotionCreateRequest;
import api.poja.app.model.Promotion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class PromotionControllerIT extends IntegrationTestSupport {

  @Test
  void admin_creates_promotion() {
    var admin = bootstrapAdminToken();

    var promotion = createPromotion(admin);

    assertThat(promotion.getId()).isNotNull();
  }

  @Test
  void create_rejects_duplicate_graduation_year() {
    var admin = bootstrapAdminToken();
    var promotion = createPromotion(admin);

    var response =
        post(
            API + "/promotions",
            admin,
            new PromotionCreateRequest("Duplicate", promotion.getGraduationYear()),
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().message()).contains("already exists");
  }

  @Test
  void list_promotions_sorted_by_graduation_year_descending() {
    var admin = bootstrapAdminToken();
    var older = createPromotion(admin);
    var newer = createPromotion(admin);

    var response =
        restTemplate.exchange(
            API + "/promotions?page=0&size=1000",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders(admin)),
            new ParameterizedTypeReference<List<Promotion>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    var ids = response.getBody().stream().map(Promotion::getId).toList();
    assertThat(ids).contains(older.getId(), newer.getId());
  }

  @Test
  void link_group_to_promotion_succeeds() {
    var admin = bootstrapAdminToken();
    var promotion = createPromotion(admin);
    var group = createGroup(admin);

    linkGroupToPromotion(admin, promotion.getId(), group.getId());
  }

  @Test
  void link_group_to_promotion_twice_conflicts() {
    var admin = bootstrapAdminToken();
    var promotion = createPromotion(admin);
    var group = createGroup(admin);
    linkGroupToPromotion(admin, promotion.getId(), group.getId());

    var response =
        post(
            API + "/promotions/" + promotion.getId() + "/groups/" + group.getId(),
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void link_group_to_unknown_promotion_returns_not_found() {
    var admin = bootstrapAdminToken();
    var group = createGroup(admin);

    var response =
        post(
            API + "/promotions/" + UUID.randomUUID() + "/groups/" + group.getId(),
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void link_unknown_group_to_promotion_returns_not_found() {
    var admin = bootstrapAdminToken();
    var promotion = createPromotion(admin);

    var response =
        post(
            API + "/promotions/" + promotion.getId() + "/groups/" + UUID.randomUUID(),
            admin,
            null,
            ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }
}
