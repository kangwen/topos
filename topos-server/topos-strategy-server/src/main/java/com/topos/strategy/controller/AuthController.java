package com.topos.strategy.controller;

import com.topos.strategy.controller.dto.LoginRequest;
import com.topos.strategy.security.userdetails.UserDetails;
import com.topos.common.api.Rsp;
import com.topos.common.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider jwtTokenProvider;

	public AuthController(
			AuthenticationManager apiAuthenticationManager,
			@Qualifier("babyJwtTokenProvider") JwtTokenProvider jwtTokenProvider) {
		this.authenticationManager = apiAuthenticationManager;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@PostMapping("/login")
	public Rsp<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password()));
		if (!(auth.getPrincipal() instanceof UserDetails ud)) {
			throw new BadCredentialsException("账号类型错误");
		}
		Map<String, Object> claims = new HashMap<>();
		claims.put("aud", "CLIENT");
		String token = jwtTokenProvider.createToken(auth.getName(), claims);
		var user = ud.getUser();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("tokenType", "Bearer");
		body.put("accessToken", token);
		body.put("id", user.getId());
		body.put("name", user.getNickname());
		return Rsp.ok(body);
	}
}
