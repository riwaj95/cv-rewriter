package com.example.cv_rewriter.repository;

import com.example.cv_rewriter.entity.CvProcessRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CvProcessRecordRepository extends JpaRepository<CvProcessRecord, Long> {

    List<CvProcessRecord> findAllByUserEmailOrderByCreatedAtDesc(String userEmail, Pageable pageable);
}

