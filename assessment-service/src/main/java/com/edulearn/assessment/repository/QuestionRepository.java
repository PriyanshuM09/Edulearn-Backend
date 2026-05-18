package com.edulearn.assessment.repository;

import com.edulearn.assessment.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository
        extends JpaRepository<Question, Integer> {

    List<Question> findByQuizQuizIdOrderByOrderIndex(
            Integer quizId);

    long countByQuizQuizId(Integer quizId);
}