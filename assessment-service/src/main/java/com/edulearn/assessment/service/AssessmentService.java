package com.edulearn.assessment.service;

import com.edulearn.assessment.dto.*;

import java.util.List;
import java.util.Map;

public interface AssessmentService {

    // Quiz operations
    QuizResponseDto createQuiz(QuizRequestDto request);
    QuizResponseDto getQuizById(Integer quizId);
    List<QuizResponseDto> getQuizzesByCourse(Integer courseId);
    QuizResponseDto updateQuiz(Integer quizId,
                               QuizRequestDto request);
    QuizResponseDto publishQuiz(Integer quizId);
    void deleteQuiz(Integer quizId);

    // Question operations
    QuestionResponseDto addQuestion(Integer quizId,
                                    QuestionRequestDto request);
    List<QuestionResponseDto> getQuestionsByQuiz(Integer quizId);
    QuestionResponseDto updateQuestion(Integer questionId,
                                       QuestionRequestDto request);
    void deleteQuestion(Integer questionId);

    // Attempt operations
    AttemptResponseDto startAttempt(Integer studentId,
                                     Integer quizId);
    AttemptResponseDto submitAttempt(Integer attemptId,
                                      AttemptRequestDto request);
    List<AttemptResponseDto> getAttemptsByStudent(
            Integer studentId);
    List<AttemptResponseDto> getAttemptsByStudentAndQuiz(
            Integer studentId, Integer quizId);
    AttemptResponseDto getBestAttempt(Integer studentId,
                                       Integer quizId);
    Integer getBestScore(Integer studentId, Integer quizId);

    Map<String, Object> getRemainingTime(Integer studentId, Integer attemptId);

    void saveProgress(Integer studentId, Integer attemptId, Map<Integer, String> answers);
}