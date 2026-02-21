package com.mediabridge.service;

import java.io.File;

import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

	public File convertToSafeAndroid(File inputFile, String mode, String jobId) throws Exception;
	
	public void processVideoAsync(String jobId, File inputFile, String mode);
	
	public File saveUploadedFile(MultipartFile file) throws Exception;

}
