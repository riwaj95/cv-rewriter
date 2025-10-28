package com.example.cv_rewriter.service;

import com.example.cv_rewriter.entity.CvProcessRecord;
import com.example.cv_rewriter.repository.CvProcessRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CvProcessRecordService {

    private static final int DEFAULT_HISTORY_LIMIT = 10;

    private final CvProcessRecordRepository cvProcessRecordRepository;

    public CvProcessRecordService(CvProcessRecordRepository cvProcessRecordRepository) {
        this.cvProcessRecordRepository = cvProcessRecordRepository;
    }

    @Transactional
    public void saveSuccessfulProcessing(String userEmail, String userName, String jobDescription, String feedbackReport) {
        CvProcessRecord record = new CvProcessRecord();
        record.setUserEmail(safeTrim(userEmail));
        record.setUserName(safeTrim(userName));
        record.setJobDescription(jobDescription);
        record.setFeedbackReport(feedbackReport);

        cvProcessRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<CvProcessRecord> fetchRecentHistory(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, DEFAULT_HISTORY_LIMIT);
        return cvProcessRecordRepository.findAllByUserEmailOrderByCreatedAtDesc(userEmail, pageable);
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}

