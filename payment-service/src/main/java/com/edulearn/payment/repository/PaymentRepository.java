package com.edulearn.payment.repository;

import com.edulearn.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    List<Payment> findByStudentId(Integer studentId);

    List<Payment> findByCourseId(Integer courseId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    // NFR — no duplicate payments
    boolean existsByStudentIdAndCourseIdAndStatus(
            Integer studentId, Integer courseId, String status);

    List<Payment> findByStudentIdAndCourseIdOrderByPaidAtDesc(Integer studentId, Integer courseId);
}