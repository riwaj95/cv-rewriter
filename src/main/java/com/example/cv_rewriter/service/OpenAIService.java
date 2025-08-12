package com.example.cv_rewriter.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class OpenAIService {

    @Value("${openai.api-key}")
    private String apiKey;

    public String enhanceCv(String jobDescription, String cvText) {
        OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(90));

        // System message to define the assistant's behavior
        String systemPrompt = "You are a professional CV enhancement expert. Rewrite the user's CV to better match "
                + "the job description while maintaining factual accuracy. Focus on: "
                + "1. Optimizing keywords from the job description\n"
                + "2. Improving professional tone\n"
                + "3. Highlighting relevant achievements\n"
                + "4. Maintaining original structure\n"
                + "5. Returning ONLY the enhanced CV text without any additional commentary or explanations";

        // User message with the actual content
        String userPrompt = "Job Description:\n" + jobDescription +
                "\n\nOriginal CV:\n" + cvText +
                "\n\nEnhanced CV:";

        List<ChatMessage> messages = List.of(
                new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt),
                new ChatMessage(ChatMessageRole.USER.value(), userPrompt)
        );

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-5-nano")  // Updated to GPT-5 :cite[1]:cite[2]
                .messages(messages)
                .maxTokens(2500)
                .temperature(0.7)
                .topP(0.9)
                .build();

        return service.createChatCompletion(request)
                .getChoices()
                .get(0)
                .getMessage()
                .getContent()
                .trim();
    }
}
