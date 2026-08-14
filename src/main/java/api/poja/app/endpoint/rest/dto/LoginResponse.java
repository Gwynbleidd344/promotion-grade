package api.poja.app.endpoint.rest.dto;

import api.poja.app.entity.enums.UserRole;

public record LoginResponse(String accessToken, String tokenType, UserRole role, long expiresIn) {}
