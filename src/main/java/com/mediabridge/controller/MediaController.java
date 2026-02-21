package com.mediabridge.controller;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import com.mediabridge.model.JobStatus;
import com.mediabridge.service.MediaService;
import com.mediabridge.service.impl.MediaServiceImpl;
import com.mediabridge.store.JobStore;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaController.class);

	@Autowired
	private MediaService mediaService;

	@PostMapping("/convert")
	public ResponseEntity<Map<String, String>> convertVideo(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "mode", defaultValue = "safe") String mode) throws Exception {

		String jobId = UUID.randomUUID().toString();

		File savedFile = mediaService.saveUploadedFile(file);

		JobStore.statusMap.put(jobId, JobStatus.PROCESSING);

		mediaService.processVideoAsync(jobId, savedFile, mode);

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
		LOGGER.info("Donwload Started");
		
		if (!JobStore.statusMap.containsKey(jobId)) {
	        return ResponseEntity.badRequest().build();
	    }
		
		File file = JobStore.resultMap.get(jobId);

		if (file == null || !file.exists()) {
			return ResponseEntity.notFound().build();
		}

		FileSystemResource resource = new FileSystemResource(file);

		LOGGER.info("Donwload Finished");
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
				.header(HttpHeaders.CONTENT_TYPE, "video/mp4").contentLength(file.length()).body(resource);
	}

	@DeleteMapping("/cancel/{jobId}")
	public ResponseEntity<String> cancelJob(@PathVariable("jobId") String jobId) {

		Process process = JobStore.processMap.get(jobId);

		if (process != null && process.isAlive()) {
			process.destroyForcibly();
		}

		File input = JobStore.inputFileMap.get(jobId);
		File output = JobStore.outputFileMap.get(jobId);

		if (input != null && input.exists())
			input.delete();
		if (output != null && output.exists())
			output.delete();

		JobStore.statusMap.put(jobId, JobStatus.CANCELLED);

		JobStore.processMap.remove(jobId);
		JobStore.inputFileMap.remove(jobId);
		JobStore.outputFileMap.remove(jobId);

		return ResponseEntity.ok("Cancelled");
	}

}
