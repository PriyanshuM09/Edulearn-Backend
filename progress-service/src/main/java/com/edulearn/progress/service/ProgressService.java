package com.edulearn.progress.service;

import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import java.util.List;

public interface ProgressService {
    ProgressResponseDto watchLesson(WatchProgressRequest request);
    Double getCourseProgressPercent(Integer studentId, Integer courseId);
    List<ProgressResponseDto> getStudentProgress(Integer studentId);
    List<ProgressResponseDto> getStudentProgress(Integer studentId, Integer courseId);
}