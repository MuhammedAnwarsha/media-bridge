package com.mediabridge.service.impl;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CleanupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);

	@Value("${media.storage.base-path}")
	private String basePath;

	@Scheduled(fixedRate = 3600000) // every 1 hour
	public void cleanupOldFiles() {

		LOGGER.info("Starting scheduled cleanup task...");

		File uploadDir = new File(basePath + "/uploads");
		File convertedDir = new File(basePath + "/converted");

		long now = System.currentTimeMillis();
		long expiryTime = 3600000; // 1 hour

		cleanupDirectory(uploadDir, now, expiryTime);
		cleanupDirectory(convertedDir, now, expiryTime);

		LOGGER.info("Cleanup task completed.");
	}

	private void cleanupDirectory(File directory, long currentTime, long expiryTime) {

		if (!directory.exists()) {
			LOGGER.warn("Directory does not exist: {}", directory.getAbsolutePath());
			return;
		}

		File[] files = directory.listFiles();

		if (files == null || files.length == 0) {
			LOGGER.info("No files found in directory: {}", directory.getAbsolutePath());
			return;
		}

		for (File file : files) {

			long fileAge = currentTime - file.lastModified();

			if (fileAge > expiryTime) {
				boolean deleted = file.delete();

				if (deleted) {
					LOGGER.info("Deleted file: {}", file.getAbsolutePath());
				} else {
					LOGGER.error("Failed to delete file: {}", file.getAbsolutePath());
				}
			}
		}
	}
}
