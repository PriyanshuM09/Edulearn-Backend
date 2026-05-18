package com.edulearn.payment.repository;

import com.edulearn.payment.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByStudentId(Integer studentId);

    Optional<Subscription> findByStudentIdAndStatus(
            Integer studentId, String status);

    List<Subscription> findByPlanType(String planType);

    List<Subscription> findByStatus(String status);

    boolean existsByStudentIdAndStatus(Integer studentId, String status);
}