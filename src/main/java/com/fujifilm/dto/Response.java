package com.fujifilm.dto;

import java.util.Map;

public class Response {

	private final String message;
	private final Boolean status;
	private final Map<String,Object> data;

	public Response(String message,Boolean status,Map<String,Object> data) {
		this.message = message;
		this.status = status;
		this.data = data;
	}

	public String getMessage() {
		return message;
	}

	public Boolean getStatus() {
		return status;
	}

	public Map<String, Object> getData() {
		return data;
	}
	
}