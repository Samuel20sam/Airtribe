package com.airtribe.taskmanager.llm;

import com.airtribe.taskmanager.exception.BadGatewayException;
import com.airtribe.taskmanager.exception.ServiceUnavailableException;
import com.airtribe.taskmanager.util.SimpleJson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API ({@code POST https://api.anthropic.com/v1/messages}) with the
 * JDK's built-in {@link HttpClient}, so no external library is needed.
 *
 * <p>This plays the part of the instructor-provided LlmClient. The API key comes from the
 * {@code LLM_API_KEY} environment variable (read by {@code ParserFactory}); it is never written
 * to a file. Without a key every call throws {@link ServiceUnavailableException} (503) instead of
 * crashing. Set {@code LLM_MODEL} to use a different Claude model.
 */
public class AnthropicLlmClient implements LlmClient {

    public static final String DEFAULT_MODEL = "claude-opus-5-5";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    public static final URI DEFAULT_ENDPOINT = URI.create("https://api.anthropic.com/v1/messages");
    private static final String API_VERSION = "2026-10-21";
    /** Lets the API retry on another model if the main one declines a request. */
    private static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";
    private static final int MAX_TOKENS = 16000;

    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final URI endpoint;
    private final HttpClient http;

    public AnthropicLlmClient(String apiKey) {
        this(apiKey, DEFAULT_MODEL, DEFAULT_TIMEOUT);
    }

    public AnthropicLlmClient(String apiKey, String model, Duration timeout) {
        this(apiKey, model, timeout, DEFAULT_ENDPOINT);
    }

    /** The endpoint can be changed for a proxy, or for a local test server. */
    public AnthropicLlmClient(String apiKey, String model, Duration timeout, URI endpoint) {
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
        this.timeout = timeout;
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String complete(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceUnavailableException("AI features are unavailable: LLM_API_KEY is not set");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header("anthropic-beta", FALLBACK_BETA)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(prompt), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new BadGatewayException("The language model did not answer within " + timeout.toSeconds() + " seconds");
        } catch (IOException e) {
            throw new BadGatewayException("Could not reach the language model: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadGatewayException("The language model call was interrupted");
        }

        int status = response.statusCode();
        if (status == 401 || status == 403) {
            throw new ServiceUnavailableException("AI features are unavailable: the provider rejected LLM_API_KEY");
        }
        if (status != 200) {
            throw new BadGatewayException("The language model provider returned HTTP " + status);
        }
        return extractText(response.body());
    }

    private String requestBody(String prompt) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", MAX_TOKENS);
        // Turning a sentence into a few fields is a simple job, so low effort keeps it fast and cheap.
        body.put("output_config", Map.of("effort", "low"));
        body.put("fallbacks", "default");
        body.put("messages", List.of(message));
        return SimpleJson.write(body);
    }

    /** Joins the text blocks of the reply. Other blocks (such as thinking) are skipped. */
    @SuppressWarnings("unchecked")
    private static String extractText(String json) {
        Map<String, Object> reply;
        try {
            reply = SimpleJson.parseObject(json);
        } catch (SimpleJson.JsonException e) {
            throw new BadGatewayException("The language model provider sent a reply that is not JSON");
        }
        if ("refusal".equals(reply.get("stop_reason"))) {
            throw new BadGatewayException("The language model declined to answer this request");
        }
        Object content = reply.get("content");
        StringBuilder text = new StringBuilder();
        if (content instanceof List) {
            for (Object block : (List<Object>) content) {
                if (block instanceof Map && "text".equals(((Map<String, Object>) block).get("type"))) {
                    Object part = ((Map<String, Object>) block).get("text");
                    if (part instanceof String) {
                        text.append((String) part);
                    }
                }
            }
        }
        if (text.length() == 0) {
            throw new BadGatewayException("The language model returned no text");
        }
        return text.toString();
    }
}
