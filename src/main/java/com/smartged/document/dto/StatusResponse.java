package com.smartged.document.dto;

import com.smartged.document.DocumentEntity;
import com.smartged.document.DocumentStatus;

import java.util.UUID;

public class StatusResponse {
	public UUID id;
	public DocumentStatus status;
	public Integer progress;
	public String message;

	public static StatusResponse from(DocumentEntity e) {
		StatusResponse r = new StatusResponse();
		r.id = e.getId();
		r.status = e.getStatus();
		return r;
	}
}


