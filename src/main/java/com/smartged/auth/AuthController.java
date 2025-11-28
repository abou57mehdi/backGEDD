package com.smartged.auth;

import com.smartged.auth.dto.LoginRequest;
import com.smartged.auth.dto.LoginResponse;
import com.smartged.auth.dto.RefreshRequest;
import com.smartged.auth.dto.RegisterRequest;
import com.smartged.security.JwtService;
import com.smartged.user.UserEntity;
import com.smartged.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserService userService;

	public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserService userService) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userService = userService;
	}

	@PostMapping("/register")
	public ResponseEntity<UserEntity> register(@RequestBody RegisterRequest request) {
		UserEntity newUser = userService.registerNewUser(request);
		return new ResponseEntity<>(newUser, HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
		Authentication auth = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username, request.password)
		);
		UserDetails user = (UserDetails) auth.getPrincipal();
		String token = jwtService.generateToken(user.getUsername(), Map.of("roles", user.getAuthorities()));
		return ResponseEntity.ok(new LoginResponse(token));
	}

	@PostMapping("/refresh")
	public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
		// For now, accept an existing valid access token and mint a new one (placeholder behavior).
		// Later, replace with a dedicated refresh token flow.
		String username = jwtService.extractUsername(request.refreshToken);
		String token = jwtService.generateToken(username, Map.of());
		return ResponseEntity.ok(new LoginResponse(token));
	}
}


