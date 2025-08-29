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
}
