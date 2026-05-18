package com.edulearn.payment.repository;

import com.edulearn.payment.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Integer> {
    List<RefundRequest> findByStatus(String status);
    List<RefundRequest> findByStudentId(Integer studentId);
}
