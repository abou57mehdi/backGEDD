package com.smartged.auth.dto;

public class LoginResponse {
	public String accessToken;
	public String tokenType = "Bearer";

	public LoginResponse(String accessToken) {
		this.accessToken = accessToken;
	}
}


