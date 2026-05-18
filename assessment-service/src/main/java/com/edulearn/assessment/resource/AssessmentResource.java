package com.edulearn.assessment.resource;

import com.edulearn.assessment.dto.*;
import com.edulearn.assessment.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Assessment Service",
     description = "APIs for quizzes and attempts")
public class AssessmentResource {

    private final AssessmentService assessmentService;

    // ── Quiz Endpoints ────────────────────────────────────────────

    // POST /api/v1/quizzes
    @PostMapping("/quizzes")
    @Operation(summary = "Create a new quiz")
    public ResponseEntity<QuizResponseDto> createQuiz(
            @Valid @RequestBody QuizRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.createQuiz(request));
    }

    // GET /api/v1/quizzes/{quizId}
    @GetMapping("/quizzes/{quizId}")
    @Operation(summary = "Get quiz by ID")
    public ResponseEntity<QuizResponseDto> getQuizById(
            @PathVariable Integer quizId) {
        return ResponseEntity.ok(
                assessmentService.getQuizById(quizId));
    }

    // GET /api/v1/quizzes/course/{courseId}
    @GetMapping("/quizzes/course/{courseId}")
    @Operation(summary = "Get all quizzes for a course")
    public ResponseEntity<List<QuizResponseDto>> getQuizzesByCourse(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(
                assessmentService.getQuizzesByCourse(courseId));
    }

    // PUT /api/v1/quizzes/{quizId}
    @PutMapping("/quizzes/{quizId}")
    @Operation(summary = "Update a quiz")
    public ResponseEntity<QuizResponseDto> updateQuiz(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuizRequestDto request) {
        return ResponseEntity.ok(
                assessmentService.updateQuiz(quizId, request));
    }

    // PUT /api/v1/quizzes/{quizId}/publish
    @PutMapping("/quizzes/{quizId}/publish")
    @Operation(summary = "Publish a quiz")
    public ResponseEntity<QuizResponseDto> publishQuiz(
            @PathVariable Integer quizId) {
        return ResponseEntity.ok(
                assessmentService.publishQuiz(quizId));
    }

    // DELETE /api/v1/quizzes/{quizId}
    @DeleteMapping("/quizzes/{quizId}")
    @Operation(summary = "Delete a quiz")
    public ResponseEntity<Map<String, String>> deleteQuiz(
            @PathVariable Integer quizId) {
        assessmentService.deleteQuiz(quizId);
        return ResponseEntity.ok(
                Map.of("message", "Quiz deleted successfully"));
    }

    // ── Question Endpoints ────────────────────────────────────────

    // POST /api/v1/quizzes/{quizId}/questions
    @PostMapping("/quizzes/{quizId}/questions")
    @Operation(summary = "Add a question to a quiz")
    public ResponseEntity<QuestionResponseDto> addQuestion(
            @PathVariable Integer quizId,
            @Valid @RequestBody QuestionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.addQuestion(
                    quizId, request));
    }

    // GET /api/v1/quizzes/{quizId}/questions
    @GetMapping("/quizzes/{quizId}/questions")
    @Operation(summary = "Get all questions for a quiz")
    public ResponseEntity<List<QuestionResponseDto>> getQuestions(
            @PathVariable Integer quizId) {
        return ResponseEntity.ok(
                assessmentService.getQuestionsByQuiz(quizId));
    }

    // PUT /api/v1/questions/{questionId}
    @PutMapping("/questions/{questionId}")
    @Operation(summary = "Update a question")
    public ResponseEntity<QuestionResponseDto> updateQuestion(
            @PathVariable Integer questionId,
            @Valid @RequestBody QuestionRequestDto request) {
        return ResponseEntity.ok(
                assessmentService.updateQuestion(
                    questionId, request));
    }

    // DELETE /api/v1/questions/{questionId}
    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Delete a question")
    public ResponseEntity<Map<String, String>> deleteQuestion(
            @PathVariable Integer questionId) {
        assessmentService.deleteQuestion(questionId);
        return ResponseEntity.ok(
                Map.of("message", "Question deleted successfully"));
    }

    // ── Attempt Endpoints ─────────────────────────────────────────

    // POST /api/v1/attempts/start
    @PostMapping("/attempts/start")
    @Operation(summary = "Start a quiz attempt")
    public ResponseEntity<AttemptResponseDto> startAttempt(
            @RequestParam Integer studentId,
            @RequestParam Integer quizId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assessmentService.startAttempt(
                    studentId, quizId));
    }

    // POST /api/v1/attempts/{attemptId}/submit
    @PostMapping("/attempts/{attemptId}/submit")
    @Operation(summary = "Submit a quiz attempt — auto grades")
    public ResponseEntity<AttemptResponseDto> submitAttempt(
            @PathVariable Integer attemptId,
            @RequestBody AttemptRequestDto request) {
        return ResponseEntity.ok(
                assessmentService.submitAttempt(
                    attemptId, request));
    }

    // GET /api/v1/attempts/student/{studentId}
    @GetMapping("/attempts/student/{studentId}")
    @Operation(summary = "Get all attempts by a student")
    public ResponseEntity<List<AttemptResponseDto>> getAttemptsByStudent(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(
                assessmentService.getAttemptsByStudent(studentId));
    }

    // GET /api/v1/attempts/student/{studentId}/quiz/{quizId}
    @GetMapping("/attempts/student/{studentId}/quiz/{quizId}")
    @Operation(summary = "Get attempts by student for a quiz")
    public ResponseEntity<List<AttemptResponseDto>> getAttemptsByStudentAndQuiz(
            @PathVariable Integer studentId,
            @PathVariable Integer quizId) {
        return ResponseEntity.ok(
                assessmentService.getAttemptsByStudentAndQuiz(
                    studentId, quizId));
    }

    // GET /api/v1/attempts/student/{studentId}/quiz/{quizId}/best
    @GetMapping("/attempts/student/{studentId}/quiz/{quizId}/best")
    @Operation(summary = "Get best attempt by student for a quiz")
    public ResponseEntity<AttemptResponseDto> getBestAttempt(
            @PathVariable Integer studentId,
            @PathVariable Integer quizId) {
        return ResponseEntity.ok(
                assessmentService.getBestAttempt(
                    studentId, quizId));
    }

    // GET /api/v1/attempts/student/{studentId}/quiz/{quizId}/score
    @GetMapping("/attempts/student/{studentId}/quiz/{quizId}/score")
    @Operation(summary = "Get best score by student for a quiz")
    public ResponseEntity<Map<String, Integer>> getBestScore(
            @PathVariable Integer studentId,
            @PathVariable Integer quizId) {
        return ResponseEntity.ok(Map.of("bestScore",
                assessmentService.getBestScore(
                    studentId, quizId)));
    }

    @GetMapping("/attempts/{attemptId}/timer")
    @Operation(summary = "Get remaining time for quiz attempt")
    public ResponseEntity<Map<String, Object>> getRemainingTime(
            @PathVariable Integer attemptId,
            @RequestParam Integer studentId) {
        return ResponseEntity.ok(assessmentService.getRemainingTime(studentId, attemptId));
    }

    @PostMapping("/attempts/{attemptId}/progress")
    @Operation(summary = "Save partial quiz answers to Redis")
    public ResponseEntity<Map<String, String>> saveProgress(
            @PathVariable Integer attemptId,
            @RequestParam Integer studentId,
            @RequestBody Map<Integer, String> answers) {
        assessmentService.saveProgress(studentId, attemptId, answers);
        return ResponseEntity.ok(Map.of("message", "Progress saved"));
    }
}