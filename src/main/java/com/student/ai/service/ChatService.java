package com.student.ai.service;

import com.student.ai.constants.SecurityConstants;
import com.student.ai.dto.request.ChatRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Validated
@Service
@RequiredArgsConstructor
public class ChatService {
    private final String SYSTEM_PROMPT = """
                        You are a travel assistant responding to users.
                        
                        You may call tools that return structured results:
                        - type: LISTING_LIST | STAY | REVIEW
                        - content: DTO data
                        
                        Your response has two parts:
                        
                        1. Start immediately with a natural, human-readable explanation.
                        - Be concise and helpful.
                        - Do not mention any phases or instructions.
                        - Do not include JSON in this part.
                        
                        2. After finishing the explanation, output exactly one JSON object as the final part of the response.
                        
                        The JSON must follow this format:
                        
                        {
                          "type": "<type from the tool result>",
                          "content": <exact tool content>
                        }
                        
                        Rules:
                        - The JSON must be based only on the actual tool result.
                        - Do not infer or construct data from the user request.
                        - If no tool was called, return:
                          { 
                            "type": "NONE",
                            "content": null
                          }
                        - Do not rename, omit, or add fields.
                        - Do not include any text after the JSON.
                        - Do not include markdown or backticks.
                        - The JSON must start on a new line with '{'.
                        """;

    private final ChatClient chatClient;
    private final ToolCallbackProvider mcpTools;

    public Flux<ServerSentEvent<String>> chat(@Valid ChatRequestDto chatRequestDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String token = String.valueOf(authentication.getCredentials());
        return chatClient.prompt(chatRequestDto.message())
                .toolCallbacks(mcpTools)
                .toolContext(Map.of(SecurityConstants.AUTHORIZATION_HEADER, token))
                .stream()
                .chatResponse()
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .map(text -> ServerSentEvent.<String>builder().data(text).build())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
