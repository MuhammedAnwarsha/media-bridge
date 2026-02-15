package com.mediabridge.controller;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import com.mediabridge.model.JobStatus;
import com.mediabridge.service.MediaService;
import com.mediabridge.store.JobStore;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

	@Autowired
	private MediaService mediaService;

	@PostMapping("/convert")
	public ResponseEntity<Map<String, String>> convertVideo(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "mode", defaultValue = "safe") String mode) {

		String jobId = UUID.randomUUID().toString();

		mediaService.processVideoAsync(jobId, file, mode);

		return ResponseEntity.ok(Map.of("jobId", jobId));
	}

	@GetMapping("/status/{jobId}")
	public ResponseEntity<Map<String, Object>> getStatus(@PathVariable("jobId") String jobId) {

		JobStatus status = JobStore.statusMap.get(jobId);
		Integer progress = JobStore.progressMap.get(jobId);

		if (status == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(Map.of("status", status.name(), "progress", progress == null ? 0 : progress));
	}

	@GetMapping("/download/{jobId}")
	public ResponseEntity<FileSystemResource> download(@PathVariable("jobId") String jobId) {

		File file = JobStore.resultMap.get(jobId);

		if (file == null || !file.exists()) {
			return ResponseEntity.notFound().build();
		}

		FileSystemResource resource = new FileSystemResource(file);

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
				.header(HttpHeaders.CONTENT_TYPE, "video/mp4").contentLength(file.length()).body(resource);
	}

}
