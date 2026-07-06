package com.mangaworldsync.service;

import io.github.aiellolorenzo23.fakedb.exception.FakeDBException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(InvalidMangaUrlException.class)
	public ResponseEntity<String> invalidMangaUrl(InvalidMangaUrlException ex) {
		return ResponseEntity.badRequest().body(ex.getMessage());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<String> missingRequestParameter(MissingServletRequestParameterException ex) {
		HttpStatus status = "token".equals(ex.getParameterName()) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body("Missing required parameter: " + ex.getParameterName());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<String> responseStatus(ResponseStatusException ex) {
		return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
	}

	@ExceptionHandler(FakeDBException.class)
	public ResponseEntity<String> storageError(FakeDBException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unable to access manga progress storage");
	}
}
