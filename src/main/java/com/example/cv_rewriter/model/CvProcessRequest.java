package com.example.cv_rewriter.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CvProcessRequest {

    @NotNull(message = "CV file cannot be null")
    private MultipartFile cvFile;

    @NotBlank(message = "Job description cannot be empty")
    private String jobDescription;

    public @NotNull(message = "CV file cannot be null") MultipartFile getCvFile() {
        return cvFile;
    }

    public void setCvFile(@NotNull(message = "CV file cannot be null") MultipartFile cvFile) {
        this.cvFile = cvFile;
    }

    public @NotBlank(message = "Job description cannot be empty") String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(@NotBlank(message = "Job description cannot be empty") String jobDescription) {
        this.jobDescription = jobDescription;
    }
}
