package com.mediabridge.controller;

import java.io.File;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.mediabridge.service.MediaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

	@Autowired
	private MediaService mediaService;

	@PostMapping("/convert")
	public ResponseEntity<FileSystemResource> convertVideo(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "mode", defaultValue = "safe") String mode) throws Exception {

		File convertedFile = mediaService.convertToSafeAndroid(file, mode);

		FileSystemResource resource = new FileSystemResource(convertedFile);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + convertedFile.getName())
				.body(resource);
	}

}
