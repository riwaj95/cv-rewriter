package com.example.cv_rewriter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaService {
    private final RestTemplate restTemplate = new RestTemplate();

    private final String ollamaBaseUrl;
    private final String defaultModel;

    public OllamaService(
            @Value("${ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
            @Value("${ollama.model:qwen2.5:0.5b}") String defaultModel
    ) {
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.defaultModel = defaultModel;
    }

    public String buildFeedbackReport(String jobDescription, String cvText) {
        String prompt = "You are a professional career coach. Review the candidate's CV against the job description and "
                + "produce a concise feedback report. The report must contain the following sections in Markdown format:\n"
                + "# CV Feedback Summary\n"
                + "## Overall Impression\n"
                + "## Strengths\n"
                + "- bullet list of strong points\n"
                + "## Gaps or Concerns\n"
                + "- bullet list of weaknesses or missing information\n"
                + "## Recommended Improvements\n"
                + "- bullet list of specific, actionable improvements\n"
                + "## Keywords To Incorporate\n"
                + "- bullet list of keywords from the job description that should appear in the CV\n"
                + "Ensure all advice is factual, based only on the provided CV and job description. Do not rewrite the CV.\n\n"
                + "Job Description:\n" + jobDescription + "\n\nCV:\n" + cvText + "\n\nFeedback Report:";

        Map<String, Object> options = new HashMap<>();
        options.put("temperature", 0.4);
        options.put("top_p", 0.9);

        return generate(prompt, options);
    }

    public String generate(String prompt, Map<String, Object> options) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", defaultModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        if (options != null && !options.isEmpty()) {
            requestBody.put("options", options);
        }

        String url = ollamaBaseUrl + "/api/generate";
        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(url, requestBody, Map.class);
        } catch (RestClientException restClientException) {
            throw new IllegalStateException("Failed to call Ollama generate endpoint", restClientException);
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException("Empty response from Ollama generate endpoint");
        }

        Object result = responseBody.get("response");
        if (result != null) {
            return result.toString().trim();
        }

        Object error = responseBody.get("error");
        if (error != null) {
            throw new IllegalStateException("Ollama error: " + error);
        }

        throw new IllegalStateException("Unexpected response from Ollama: " + responseBody);
    }
}
