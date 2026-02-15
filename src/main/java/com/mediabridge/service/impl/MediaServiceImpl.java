package com.mediabridge.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.scheduling.annotation.Async;

import com.mediabridge.model.JobStatus;
import com.mediabridge.service.MediaService;
import com.mediabridge.store.JobStore;

@Service
public class MediaServiceImpl implements MediaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);

	private final String basePath;

	public MediaServiceImpl(@Value("${media.storage.base-path}") String basePath) {
		this.basePath = basePath;
	}

	@Override
	public File convertToSafeAndroid(MultipartFile file, String mode, String jobId) throws Exception {

		File uploadDir = new File(basePath + File.separator + "uploads");
		File outputDir = new File(basePath + File.separator + "converted");

		if (!uploadDir.exists())
			uploadDir.mkdirs();
		if (!outputDir.exists())
			outputDir.mkdirs();

		String originalFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
		File inputFile = new File(uploadDir, originalFilename);

		file.transferTo(inputFile);

		String outputFilename = "converted_" + originalFilename.replace(".mov", ".mp4");
		File outputFile = new File(outputDir, outputFilename);

		String command;

		if ("high".equalsIgnoreCase(mode)) {
			command = String.format(
					"ffmpeg -i \"%s\" -vf \"scale=1920:-2,format=yuv420p\" " + "-c:v libx264 -preset fast -crf 20 "
							+ "-profile:v high -level 4.1 -movflags +faststart " + "-c:a aac -b:a 160k \"%s\"",
					inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
		} else {
			command = String.format(
					"ffmpeg -i \"%s\" -vf \"scale=1280:-2,format=yuv420p\" " + "-c:v libx264 -preset fast -crf 22 "
							+ "-profile:v baseline -level 3.0 -movflags +faststart " + "-c:a aac -b:a 128k \"%s\"",
					inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
		}

		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
		builder.redirectErrorStream(true);
		Process process = builder.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

		String line;
		double totalDuration = 0;

		while ((line = reader.readLine()) != null) {

			LOGGER.info("FFmpeg: {}", line);

			// Extract total duration
			if (line.contains("Duration:")) {
				totalDuration = parseDuration(line);
			}

			// Extract current processed time
			if (line.contains("time=") && totalDuration > 0) {
				double currentTime = parseCurrentTime(line);
				int progress = (int) ((currentTime / totalDuration) * 100);
				progress = Math.min(progress, 100);

				JobStore.progressMap.put(jobId, progress);
			}
		}

		int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new RuntimeException("FFmpeg conversion failed");
		}

		return outputFile;
	}

	@Override
	@Async
	public void processVideoAsync(String jobId, MultipartFile file, String mode) {

		try {

			JobStore.statusMap.put(jobId, JobStatus.PROCESSING);
			JobStore.progressMap.put(jobId, 0);

			File result = convertToSafeAndroid(file, mode, jobId);

			JobStore.statusMap.put(jobId, JobStatus.COMPLETED);
			JobStore.progressMap.put(jobId, 100);
			JobStore.resultMap.put(jobId, result);

		} catch (Exception e) {
			LOGGER.error("Conversion failed", e);
			JobStore.statusMap.put(jobId, JobStatus.FAILED);
		}
	}


	private double parseDuration(String line) {
		try {
			String duration = line.split("Duration:")[1].split(",")[0].trim();
			return convertToSeconds(duration);
		} catch (Exception e) {
			return 0;
		}
	}

	private double parseCurrentTime(String line) {
		try {
			String timePart = line.split("time=")[1].split(" ")[0];
			return convertToSeconds(timePart);
		} catch (Exception e) {
			return 0;
		}
	}

	private double convertToSeconds(String time) {
		String[] parts = time.split(":");
		return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Double.parseDouble(parts[2]);
	}
}
