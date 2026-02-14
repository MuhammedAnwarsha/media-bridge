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

import com.mediabridge.service.MediaService;

@Service
public class MediaServiceImpl implements MediaService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServiceImpl.class);

	private final String basePath;

	public MediaServiceImpl(@Value("${media.storage.base-path}") String basePath) {
		this.basePath = basePath;
	}

	@Override
	public File convertToSafeAndroid(MultipartFile file, String mode) throws Exception {

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
							+ "-profile:v high -level 4.1 " + "-movflags +faststart " + "-c:a aac -b:a 160k \"%s\"",
					inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
		} else {

			// SAFE MODE
			command = String.format(
					"ffmpeg -i \"%s\" -vf \"scale=1280:-2,format=yuv420p\" " + "-c:v libx264 -preset fast -crf 22 "
							+ "-profile:v baseline -level 3.0 " + "-movflags +faststart " + "-c:a aac -b:a 128k \"%s\"",
					inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
		}

		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
		builder.redirectErrorStream(true);
		Process process = builder.start();

		Thread outputThread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {

				String line;
				while ((line = reader.readLine()) != null) {
					LOGGER.info("FFmpeg: {}", line);
				}

			} catch (Exception e) {
				LOGGER.error("Error reading FFmpeg output", e);
			}
		});

		outputThread.start();

		int exitCode = process.waitFor();

		outputThread.join(); // wait for logging to finish

		LOGGER.info("FFmpeg process exited with code: {}", exitCode);

		if (exitCode != 0) {
			throw new RuntimeException("FFmpeg conversion failed");
		}

		return outputFile;
	}

}
