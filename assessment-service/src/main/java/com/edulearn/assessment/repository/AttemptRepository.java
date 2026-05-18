package com.edulearn.assessment.repository;

import com.edulearn.assessment.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository
        extends JpaRepository<Attempt, Integer> {

    List<Attempt> findByStudentId(Integer studentId);

    List<Attempt> findByQuizId(Integer quizId);

    List<Attempt> findByStudentIdAndQuizId(
            Integer studentId, Integer quizId);

    long countByStudentIdAndQuizId(
            Integer studentId, Integer quizId);

    Optional<Attempt> findFirstByStudentIdAndQuizIdAndIsSubmittedFalse(
            Integer studentId, Integer quizId);

    Optional<Attempt> findTopByStudentIdAndQuizIdOrderByScoreDesc(
            Integer studentId, Integer quizId);
}