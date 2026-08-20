package api.poja.app.web.dto;

import java.util.UUID;

public class WebDtos {

  public record LoginForm(String username, String password) {}

  public record LoginResponse(
      String accessToken, String tokenType, String role, Integer expiresIn) {}

  public record Promotion(UUID id, String name, Integer graduationYear) {}

  public record GraduationListExport(
      UUID promotionId, String s3Key, String downloadUrl, String generatedAt) {}

  public record ApiError(Integer status, String message, String timestamp) {}
}
