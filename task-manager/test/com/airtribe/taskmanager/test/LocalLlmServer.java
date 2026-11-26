package com.airtribe.taskmanager.test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * A tiny HTTP server on localhost that answers like the Anthropic Messages API, so the real
 * {@code AnthropicLlmClient} can be tested without internet or an API key. It records the last
 * request it received.
 */
public class LocalLlmServer implements AutoCloseable {

    private final HttpServer server;
    private volatile String lastBody;
    private volatile String lastApiKey;
    private volatile String lastVersion;

    public LocalLlmServer(int status, String responseBody, long delayMillis) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/messages", exchange -> {
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastApiKey = exchange.getRequestHeaders().getFirst("x-api-key");
            lastVersion = exchange.getRequestHeaders().getFirst("anthropic-version");
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("content-type", "application/json");
            try {
                exchange.sendResponseHeaders(status, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            } catch (IOException clientGone) {
                // The client timed out and closed the connection; nothing to do.
            }
        });
        server.start();
    }

    public URI uri() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/messages");
    }

    public String getLastBody() {
        return lastBody;
    }

    public String getLastApiKey() {
        return lastApiKey;
    }

    public String getLastVersion() {
        return lastVersion;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
