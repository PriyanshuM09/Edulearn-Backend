package com.edulearn.course.repository;

import com.edulearn.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    List<Course> findByTitleContainingIgnoreCase(String title);

    List<Course> findByCategoryIgnoreCase(String category);

    List<Course> findByInstructorId(Integer instructorId);

    List<Course> findByLevelIgnoreCase(String level);

    List<Course> findByApprovalStatusAndIsPublished(String approvalStatus, Boolean isPublished);

    List<Course> findByApprovalStatus(String approvalStatus);

    @Query("SELECT c FROM Course c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Course> searchByKeyword(@Param("keyword") String keyword);

    List<Course> findByPriceLessThanEqual(Double price);

    @Query("SELECT c FROM Course c WHERE c.isPublished = true AND c.approvalStatus = 'APPROVED' ORDER BY c.createdAt DESC")
    List<Course> findFeaturedCourses();
}