package com.mediabridge.store;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mediabridge.model.JobStatus;

public class JobStore {
	
	public static Map<String, JobStatus> statusMap = new ConcurrentHashMap<>();
    public static Map<String, File> resultMap = new ConcurrentHashMap<>();
    public static Map<String, Integer> progressMap = new ConcurrentHashMap<>();
    public static Map<String, Process> processMap = new ConcurrentHashMap<>();
    public static Map<String, File> inputFileMap = new ConcurrentHashMap<>();
    public static Map<String, File> outputFileMap = new ConcurrentHashMap<>();

}
