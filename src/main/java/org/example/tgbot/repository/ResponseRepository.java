package org.example.tgbot.repository;

import org.example.tgbot.model.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    List<Response> findByUserIdOrderByIdDesc(Long userId);
    List<Response> deleteByUserIdAndEmailIsNullOrRatingIsNull(Long userId);
    List<Response> findByIsCompletedFalseAndCreatedAtBefore(LocalDateTime threshold);
    List<Response> findByIsCompletedTrue();
}