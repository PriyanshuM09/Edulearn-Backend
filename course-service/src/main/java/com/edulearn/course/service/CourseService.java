package com.edulearn.course.service;

import com.edulearn.course.dto.CourseRequestDto;
import com.edulearn.course.dto.CourseResponseDto;
import com.edulearn.course.dto.RejectionRequest;

import java.util.List;

public interface CourseService {

    CourseResponseDto createCourse(CourseRequestDto requestDto);

    List<CourseResponseDto> getAllCourses(); // public catalog: APPROVED + Published

    CourseResponseDto getCourseById(Integer id);

    List<CourseResponseDto> getCoursesByCategory(String category);

    List<CourseResponseDto> getCoursesByInstructor(Integer instructorId);

    List<CourseResponseDto> searchCourses(String keyword);

    CourseResponseDto updateCourse(Integer id, CourseRequestDto requestDto);

    void deleteCourse(Integer id);

    List<CourseResponseDto> getFeaturedCourses();

    // New publishing flow
    CourseResponseDto submitForReview(Integer id);
    CourseResponseDto approveCourse(Integer id);
    CourseResponseDto rejectCourse(Integer id, RejectionRequest rejectionRequest);
    CourseResponseDto unpublishCourse(Integer id);

    // Admin/Instructor specific
    List<CourseResponseDto> getPendingCourses();
    List<CourseResponseDto> getAllCoursesForAdmin();
}