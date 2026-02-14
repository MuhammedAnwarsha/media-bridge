package com.mediabridge.service;

import java.io.File;

import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

	public File convertToSafeAndroid(MultipartFile file, String mode) throws Exception;

}
