package com.topos.strategy.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WsLoadTestTokenRequest(
		@NotBlank @Size(max = 256) String clientId) {
}
