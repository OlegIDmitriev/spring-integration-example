package com.dmitriev.i.oleg.si.example.stub.handler;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class RestStubber {
    private final String correlationKey;
    private final Map<Key, RestResponse> responseMap = new ConcurrentHashMap<>();
    private final Map<String, String> urlMatchers = new ConcurrentHashMap<>();
    private static final String DEFAULT_ID = "";

    public void stub(
            RequestMethod method,
            String path,
            String correlationValue
    ) {
        stub(method, path, correlationValue, 200, null, MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    }

    public void stub(
            RequestMethod method,
            String path,
            String correlationValue,
            String responseBody
    ) {
        stub(method, path, correlationValue, 200, responseBody, MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
    }

    public void stub(
            RequestMethod method,
            String path,
            String correlationValue,
            String responseBody,
            MediaType mediaType,
            Charset charset
    ) {
        stub(method, path, correlationValue, 200, responseBody, mediaType, charset);
    }

    public void stub(
            RequestMethod method,
            String path,
            String correlationValue,
            int responseStatus,
            String responseBody,
            MediaType mediaType,
            Charset charset
    ) {
        var correlationVal = StringUtils.isBlank(correlationValue)
                ? DEFAULT_ID
                : correlationValue;
        log.info("Stub answer for {} {}, {}={}", method, path, correlationKey, correlationVal);

        var key = new Key(correlationValue, normalizeForStub(path), method.name());
        var response = new RestResponse(responseStatus, responseBody, mediaType, charset);
        responseMap.put(key, response);
    }

    public ResponseEntity<Object> getResponse(HttpServletRequest request) {
        var method = request.getMethod();
        var path = normalizeForRequest(request.getRequestURI());

        var correlationId = request.getHeader(correlationKey);
        if (correlationId == null) {
            correlationId = DEFAULT_ID;
        }

        log.info("{} {}, {}={}. Stubbing answer", method, path, correlationKey, correlationId);
        var response = responseMap.get(new Key(correlationId, path, method));
        if (response == null && !DEFAULT_ID.equals(correlationId)) {
            log.info("{} {}, Not found answer for correlationId={}. Search default answer",
                    method, path, correlationId);
            response = responseMap.get(new Key(DEFAULT_ID, path, method));
        }

        if (response == null) {
            log.warn("!!! NOT FOUND MATCHING STUB ANSWER. {} {}, {}={}", method, path, correlationKey, correlationId);
            return ResponseEntity
                    .status(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Stub not found for %s %s, %s=%s".formatted(method, path, correlationKey, correlationId));
        }

        return ResponseEntity
                .status(response.responseStatus)
                .contentType(response.mediaType)
                .body(getResponseBody(response));
    }

    private Object getResponseBody(RestResponse response) {
        var mediaType = response.mediaType;
        var body = response.responseBody;

        if (mediaType == MediaType.APPLICATION_JSON) return body;
        if (mediaType == MediaType.TEXT_PLAIN) return body;
        if (mediaType == MediaType.APPLICATION_OCTET_STREAM) return body.getBytes(response.charset);

        throw new IllegalArgumentException("Unsupported Media Type: " + mediaType);
    }

    private String normalize(String path) {
        return StringUtils.strip(path, "/").replaceFirst("^stub/", "");
    }

    private String normalizeForStub(String path) {
        var normalized = normalize(path);
        if (normalized.endsWith(".*")) {
            var commonPath = normalize(StringUtils.substringBefore(normalized, ".*"));
            urlMatchers.put(normalized, commonPath);
            return commonPath;
        } else {
            return normalized;
        }
    }

    private String normalizeForRequest(String path) {
        var normalized = normalize(path);
        var matcher = urlMatchers.entrySet().stream()
                .filter(entry -> normalized.matches(entry.getKey()))
                .findFirst();

        if (matcher.isPresent()) {
            return matcher.get().getValue();
        }

        return normalized;
    }

    record Key(String correlationValue, String path, String method) {
    }

    record RestResponse(
            int responseStatus,
            String responseBody,
            MediaType mediaType,
            Charset charset
    ) {
    }
}
