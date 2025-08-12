package com.example.cv_rewriter.service;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class OpenAIService {

    @Value("${openai.api-key}")
    private String apiKey;

    public String enhanceCv(String jobDescription, String cvText) {
        OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(60));

        String prompt = "Revise the following CV to better match this job description. " +
                "Focus on skills, experience, and keywords. Return ONLY the enhanced CV text " +
                "without any additional commentary or explanations.\n\n" +
                "Job Description:\n" + jobDescription + "\n\n" +
                "Original CV:\n" + cvText + "\n\n" +
                "Enhanced CV:";

        CompletionRequest request = CompletionRequest.builder()
                .model("text-davinci-003")
                .prompt(prompt)
                .maxTokens(2000)
                .temperature(0.7)
                .build();

        return service.createCompletion(request).getChoices().get(0).getText().trim();
    }
}
