package com.edulearn.course.service.impl;

import com.edulearn.course.dto.CourseRequestDto;
import com.edulearn.course.dto.CourseResponseDto;
import com.edulearn.course.dto.RejectionRequest;
import com.edulearn.course.dto.UserDto;
import com.edulearn.course.dto.UserProfileDto;
import com.edulearn.course.entity.Course;
import com.edulearn.course.exception.CourseNotFoundException;
import com.edulearn.course.mapper.CourseMapper;
import com.edulearn.course.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CourseServiceImpl courseService;

    @Test
    @DisplayName("Test Create Course - Success")
    void createCourse_Success() {
        // given
        CourseRequestDto request = new CourseRequestDto();
        request.setTitle("New Course");
        Course course = new Course();
        course.setCourseId(1);
        course.setTitle("New Course");
        CourseResponseDto response = new CourseResponseDto();
        response.setCourseId(1);
        response.setTitle("New Course");

        when(courseMapper.toEntity(any(CourseRequestDto.class))).thenReturn(course);
        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(courseMapper.toDto(any(Course.class))).thenReturn(response);

        // when
        CourseResponseDto result = courseService.createCourse(request);

        // then
        assertNotNull(result);
        assertEquals("New Course", result.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void getAllCourses_ReturnsCachedValue() {
        List<CourseResponseDto> cached = List.of(new CourseResponseDto());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("courses:all")).thenReturn(cached);

        List<CourseResponseDto> result = courseService.getAllCourses();

        assertSame(cached, result);
        verify(courseRepository, never()).findByApprovalStatusAndIsPublished(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("Test Get All Courses - Success")
    void getAllCourses_Success() {
        // given
        Course course = new Course();
        course.setApprovalStatus("APPROVED");
        course.setIsPublished(true);
        CourseResponseDto response = new CourseResponseDto();

        when(courseRepository.findByApprovalStatusAndIsPublished("APPROVED", true)).thenReturn(List.of(course));
        when(courseMapper.toDto(any(Course.class))).thenReturn(response);

        // when
        List<CourseResponseDto> result = courseService.getAllCourses();

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Get Course By ID - Success")
    void getCourseById_Success() {
        // given
        Integer id = 1;
        Course course = new Course();
        course.setCourseId(id);
        CourseResponseDto response = new CourseResponseDto();
        response.setCourseId(id);

        when(courseRepository.findById(id)).thenReturn(Optional.of(course));
        when(courseMapper.toDto(any(Course.class))).thenReturn(response);

        // when
        CourseResponseDto result = courseService.getCourseById(id);

        // then
        assertNotNull(result);
        assertEquals(id, result.getCourseId());
    }

    @Test
    void getCourseById_SetsExternalFieldsAndCachesResponse() {
        Course course = approvedCourse(1);
        CourseResponseDto response = new CourseResponseDto();
        response.setCourseId(1);
        UserProfileDto profile = new UserProfileDto();
        profile.setFullName("Ada Teacher");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(courseMapper.toDto(course)).thenReturn(response);
        when(restTemplate.getForObject("http://auth-service/api/v1/auth/profile/7", UserProfileDto.class))
                .thenReturn(profile);
        when(restTemplate.getForObject("http://enrollment-service/api/v1/enrollments/course/1/count", Map.class))
                .thenReturn(Map.of("count", 12));

        CourseResponseDto result = courseService.getCourseById(1);

        assertEquals("Ada Teacher", result.getInstructorName());
        assertEquals(12L, result.getEnrollmentCount());
        verify(valueOperations).set("course:1", response, 10, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("Test Get Course By ID - Not Found")
    void getCourseById_NotFound() {
        // given
        Integer id = 99;
        when(courseRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThrows(CourseNotFoundException.class, () -> courseService.getCourseById(id));
    }

    @Test
    void filtersOnlyPublishedApprovedCoursesForCategoryAndSearch() {
        Course visible = approvedCourse(1);
        Course hidden = approvedCourse(2);
        hidden.setIsPublished(false);
        CourseResponseDto dto = new CourseResponseDto();
        when(courseRepository.findByCategoryIgnoreCase("java")).thenReturn(List.of(visible, hidden));
        when(courseRepository.searchByKeyword("java")).thenReturn(List.of(hidden, visible));
        when(courseMapper.toDto(visible)).thenReturn(dto);

        assertEquals(1, courseService.getCoursesByCategory("java").size());
        assertEquals(1, courseService.searchCourses("java").size());
    }

    @Test
    void instructorAdminPendingAndFeaturedQueriesMapResults() {
        Course course = approvedCourse(1);
        CourseResponseDto dto = new CourseResponseDto();
        when(courseMapper.toDto(course)).thenReturn(dto);
        when(courseRepository.findByInstructorId(7)).thenReturn(List.of(course));
        when(courseRepository.findByApprovalStatus("PENDING")).thenReturn(List.of(course));
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(courseRepository.findFeaturedCourses()).thenReturn(List.of(course));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertEquals(1, courseService.getCoursesByInstructor(7).size());
        assertEquals(1, courseService.getPendingCourses().size());
        assertEquals(1, courseService.getAllCoursesForAdmin().size());
        assertEquals(1, courseService.getFeaturedCourses().size());
        verify(valueOperations).set(eq("courses:featured"), any(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void updateSubmitApproveRejectUnpublishAndDeleteCoverStateChanges() {
        Course course = approvedCourse(1);
        CourseRequestDto request = new CourseRequestDto();
        CourseResponseDto dto = new CourseResponseDto();
        RejectionRequest rejection = new RejectionRequest();
        rejection.setRejectionReason("Needs more depth");
        UserDto admin = new UserDto();
        admin.setUserId(99);

        when(courseRepository.findById(1)).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);
        when(courseMapper.toDto(course)).thenReturn(dto);
        when(courseRepository.existsById(1)).thenReturn(true);
        when(restTemplate.exchange(
                eq("http://auth-service/api/v1/auth/admin/users/role/ADMIN"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(admin)));

        assertSame(dto, courseService.updateCourse(1, request));
        assertSame(dto, courseService.submitForReview(1));
        assertEquals("PENDING", course.getApprovalStatus());
        assertFalse(course.getIsPublished());

        assertSame(dto, courseService.approveCourse(1));
        assertEquals("APPROVED", course.getApprovalStatus());
        assertTrue(course.getIsPublished());

        assertSame(dto, courseService.rejectCourse(1, rejection));
        assertEquals("REJECTED", course.getApprovalStatus());
        assertEquals("Needs more depth", course.getRejectionReason());

        assertSame(dto, courseService.unpublishCourse(1));
        assertEquals("PENDING", course.getApprovalStatus());
        assertFalse(course.getIsPublished());

        courseService.deleteCourse(1);
        verify(courseRepository).deleteById(1);
        verify(restTemplate, atLeastOnce()).postForObject(eq("http://notification-service/api/v1/notifications/bulk"), any(), eq(Object.class));
    }

    @Test
    void notFoundBranchesThrowForMutatingOperations() {
        when(courseRepository.findById(404)).thenReturn(Optional.empty());
        when(courseRepository.existsById(404)).thenReturn(false);
        CourseRequestDto request = new CourseRequestDto();
        RejectionRequest rejection = new RejectionRequest();

        assertThrows(CourseNotFoundException.class, () -> courseService.updateCourse(404, request));
        assertThrows(CourseNotFoundException.class, () -> courseService.submitForReview(404));
        assertThrows(CourseNotFoundException.class, () -> courseService.approveCourse(404));
        assertThrows(CourseNotFoundException.class, () -> courseService.rejectCourse(404, rejection));
        assertThrows(CourseNotFoundException.class, () -> courseService.unpublishCourse(404));
        assertThrows(CourseNotFoundException.class, () -> courseService.deleteCourse(404));
    }

    private Course approvedCourse(Integer id) {
        Course course = new Course();
        course.setCourseId(id);
        course.setTitle("Java");
        course.setInstructorId(7);
        course.setApprovalStatus("APPROVED");
        course.setIsPublished(true);
        return course;
    }
}
