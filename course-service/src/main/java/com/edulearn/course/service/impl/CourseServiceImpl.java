package com.edulearn.course.service.impl;

import com.edulearn.course.dto.*;
import com.edulearn.course.entity.Course;
import com.edulearn.course.exception.CourseNotFoundException;
import com.edulearn.course.mapper.CourseMapper;
import com.edulearn.course.repository.CourseRepository;
import com.edulearn.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public CourseResponseDto createCourse(CourseRequestDto requestDto) {
        log.info("Creating course with title: {}", requestDto.getTitle());
        Course course = courseMapper.toEntity(requestDto);
        course.setApprovalStatus("PENDING");
        course.setIsPublished(false);
        Course saved = courseRepository.save(course);
        
        // Invalidate catalog caches
        invalidateCaches(null);
        
        return mapToDto(saved);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CourseResponseDto> getAllCourses() {
        String key = "courses:all";
        try {
            List<CourseResponseDto> cached = (List<CourseResponseDto>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Returning from cache: {}", key);
                return cached;
            }
        } catch (Exception e) {
            log.error("Redis error in getAllCourses: {}", e.getMessage());
        }

        log.info("Cache miss for: {}. Fetching from MySQL", key);
        List<CourseResponseDto> courses = courseRepository.findByApprovalStatusAndIsPublished("APPROVED", true)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(key, courses, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis save failed for {}: {}", key, e.getMessage());
        }
        
        return courses;
    }

    @Override
    public CourseResponseDto getCourseById(Integer id) {
        String key = "course:" + id;
        try {
            CourseResponseDto cached = (CourseResponseDto) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Returning from cache: {}", key);
                return cached;
            }
        } catch (Exception e) {
            log.error("Redis error in getCourseById: {}", e.getMessage());
        }

        log.info("Cache miss for: {}. Fetching from MySQL", key);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        CourseResponseDto dto = mapToDto(course);

        try {
            redisTemplate.opsForValue().set(key, dto, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis save failed for {}: {}", key, e.getMessage());
        }
        
        return dto;
    }

    @Override
    public List<CourseResponseDto> getCoursesByCategory(String category) {
        log.info("Fetching courses by category: {}", category);
        return courseRepository.findByCategoryIgnoreCase(category)
                .stream()
                .filter(c -> "APPROVED".equals(c.getApprovalStatus()) && Boolean.TRUE.equals(c.getIsPublished()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDto> getCoursesByInstructor(Integer instructorId) {
        log.info("Fetching all courses for instructor ID: {}", instructorId);
        return courseRepository.findByInstructorId(instructorId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDto> searchCourses(String keyword) {
        log.info("Searching courses with keyword: {}", keyword);
        return courseRepository.searchByKeyword(keyword)
                .stream()
                .filter(c -> "APPROVED".equals(c.getApprovalStatus()) && Boolean.TRUE.equals(c.getIsPublished()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseResponseDto updateCourse(Integer id, CourseRequestDto requestDto) {
        log.info("Updating course with ID: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        courseMapper.updateEntityFromDto(requestDto, course);
        Course saved = courseRepository.save(course);
        
        invalidateCaches(id);
        
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public CourseResponseDto submitForReview(Integer id) {
        log.info("Submitting course {} for review", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        course.setApprovalStatus("PENDING");
        course.setIsPublished(false);
        Course saved = courseRepository.save(course);

        invalidateCaches(id);

        // Notify Admins
        try {
            List<UserDto> admins = restTemplate.exchange(
                    "http://auth-service/api/v1/auth/admin/users/role/ADMIN",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserDto>>() {}
            ).getBody();

            if (admins != null && !admins.isEmpty()) {
                NotificationRequestDto notification = NotificationRequestDto.builder()
                        .recipientIds(admins.stream().map(UserDto::getUserId).collect(Collectors.toList()))
                        .recipientRole("ADMIN")
                        .type("COURSE_SUBMISSION")
                        .title("New Course Submission")
                        .message("Course '" + course.getTitle() + "' has been submitted for review.")
                        .channel("BOTH")
                        .build();
                restTemplate.postForObject("http://notification-service/api/v1/notifications/bulk", notification, Object.class);
            }
        } catch (Exception e) {
            log.error("Failed to send notification to admins: {}", e.getMessage());
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public CourseResponseDto approveCourse(Integer id) {
        log.info("Approving course: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        course.setApprovalStatus("APPROVED");
        course.setIsPublished(true);
        course.setRejectionReason(null);
        Course saved = courseRepository.save(course);

        invalidateCaches(id);

        // Notify Instructor
        sendNotificationToInstructor(course, "Course Approved", "Congratulations! Your course '" + course.getTitle() + "' has been approved and published.");

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public CourseResponseDto rejectCourse(Integer id, RejectionRequest rejectionRequest) {
        log.info("Rejecting course: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        course.setApprovalStatus("REJECTED");
        course.setIsPublished(false);
        course.setRejectionReason(rejectionRequest.getRejectionReason());
        Course saved = courseRepository.save(course);

        invalidateCaches(id);

        // Notify Instructor
        sendNotificationToInstructor(course, "Course Rejected", "Your course '" + course.getTitle() + "' was rejected. Reason: " + rejectionRequest.getRejectionReason());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public CourseResponseDto unpublishCourse(Integer id) {
        log.info("Unpublishing course: {}", id);
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        course.setIsPublished(false);
        course.setApprovalStatus("PENDING"); // Re-approval needed
        Course saved = courseRepository.save(course);
        
        invalidateCaches(id);
        
        return mapToDto(saved);
    }

    @Override
    public List<CourseResponseDto> getPendingCourses() {
        log.info("Fetching pending courses for admin");
        return courseRepository.findByApprovalStatus("PENDING")
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseResponseDto> getAllCoursesForAdmin() {
        log.info("Fetching all courses for admin management");
        return courseRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCourse(Integer id) {
        log.info("Deleting course with ID: {}", id);
        if (!courseRepository.existsById(id)) {
            throw new CourseNotFoundException(id);
        }
        courseRepository.deleteById(id);
        invalidateCaches(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CourseResponseDto> getFeaturedCourses() {
        String key = "courses:featured";
        try {
            List<CourseResponseDto> cached = (List<CourseResponseDto>) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.info("Returning from cache: {}", key);
                return cached;
            }
        } catch (Exception e) {
            log.error("Redis error in getFeaturedCourses: {}", e.getMessage());
        }

        log.info("Cache miss for: {}. Fetching from MySQL", key);
        List<CourseResponseDto> courses = courseRepository.findFeaturedCourses()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(key, courses, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Redis save failed for {}: {}", key, e.getMessage());
        }
        
        return courses;
    }

    private void invalidateCaches(Integer courseId) {
        try {
            redisTemplate.delete("courses:all");
            redisTemplate.delete("courses:featured");
            if (courseId != null) {
                redisTemplate.delete("course:" + courseId);
            }
            log.info("Cache invalidated for course catalog");
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", e.getMessage());
        }
    }

    private void sendNotificationToInstructor(Course course, String title, String message) {
        try {
            NotificationRequestDto notification = NotificationRequestDto.builder()
                    .recipientIds(Collections.singletonList(course.getInstructorId()))
                    .recipientRole("INSTRUCTOR")
                    .type("COURSE_STATUS")
                    .title(title)
                    .message(message)
                    .channel("BOTH")
                    .build();
            restTemplate.postForObject("http://notification-service/api/v1/notifications/bulk", notification, Object.class);
        } catch (Exception e) {
            log.error("Failed to send notification to instructor: {}", e.getMessage());
        }
    }

    private CourseResponseDto mapToDto(Course course) {
        CourseResponseDto dto = courseMapper.toDto(course);
        
        // Fetch Instructor Name
        try {
            UserProfileDto instructor = restTemplate.getForObject(
                    "http://auth-service/api/v1/auth/profile/" + course.getInstructorId(),
                    UserProfileDto.class
            );
            if (instructor != null) {
                dto.setInstructorName(instructor.getFullName());
            }
        } catch (Exception e) {
            log.warn("Could not fetch instructor name for course {}: {}", course.getCourseId(), e.getMessage());
        }

        // Fetch Enrollment Count from Enrollment Service
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> countData = restTemplate.getForObject(
                    "http://enrollment-service/api/v1/enrollments/course/" + course.getCourseId() + "/count",
                    Map.class
            );
            if (countData != null && countData.containsKey("count")) {
                // Handle both Integer and Long cases from JSON
                Object count = countData.get("count");
                dto.setEnrollmentCount(Long.valueOf(count.toString()));
            } else {
                dto.setEnrollmentCount(0L);
            }
        } catch (Exception e) {
            log.warn("Could not fetch enrollment count for course {}: {}", course.getCourseId(), e.getMessage());
            dto.setEnrollmentCount(0L);
        }

        return dto;
    }
}