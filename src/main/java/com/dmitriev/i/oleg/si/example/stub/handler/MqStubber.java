package com.dmitriev.i.oleg.si.example.stub.handler;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.MessageHeaders;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
public class MqStubber {
    private final String correlationKey;
    private final Map<Key, List<Response>> responseMap = new ConcurrentHashMap<>();
    private final Map<Key, List<String>> requestMap = new ConcurrentHashMap<>();
    private static final String DEFAULT_ID = "";

    public void stub(
            String queue,
            String correlationValue,
            String responseBody
    ) {
        stub(queue, correlationValue, responseBody, PayloadType.JSON, null);
    }

    public void stub(
            String queue,
            String correlationValue,
            String responseBody,
            String matchingExpression
    ) {
        stub(queue, correlationValue, responseBody, PayloadType.JSON, matchingExpression);
    }

    public void stub(
            String queue,
            String correlationValue,
            String responseBody,
            PayloadType payloadType
    ) {
        stub(queue, correlationValue, responseBody, payloadType, null);
    }

    public void stub(
            String queue,
            String correlationValue,
            String responseBody,
            PayloadType payloadType,
            String matchingExpression
    ) {
        var correlationVal = StringUtils.isNotBlank(correlationValue)
                ? correlationValue
                : DEFAULT_ID;
        log.info("Stub answer for queue={}, {}={}, matchingPath={}", queue, correlationKey, correlationVal, matchingExpression);

        var key = new Key(queue, correlationVal);
        var response = new Response(queue, correlationVal, responseBody, payloadType, StringUtils.trimToNull(matchingExpression));
        var responseList = responseMap.computeIfAbsent(key, k -> new ArrayList<>());
        responseList.add(response);
    }

    public String getResponseBody(String queue, String requestBody, Map<String, Object> headers) {
        var correlationId = (String) headers.get(correlationKey);
        if (correlationId == null) {
            correlationId = DEFAULT_ID;
        }

        log.info("{}, {}={}. Stubbing answer", queue, correlationKey, correlationId);
        var key = new Key(queue, requestBody);
        var receivedRequests = requestMap.computeIfAbsent(key, k -> new ArrayList<>());
        receivedRequests.add(requestBody);

        var response = searchMatchingResponse(key, requestBody);
        if (response == null && !DEFAULT_ID.equals(correlationId)) {
            log.info("{}, Not found answer for correlationId={}. Search default answer", queue, correlationId);
            response = searchMatchingResponse(new Key(queue, DEFAULT_ID), requestBody);
        }

        if (response == null) {
            log.warn("!!! {}, NOT FOUND MATCHING ANSWER", queue);
            return null;
        }

        return response.responseBody;
    }

    private Response searchMatchingResponse(Key key, String requestBody) {
        var savedResponses = responseMap.computeIfAbsent(key, k -> Collections.emptyList());
        var response = savedResponses.stream()
                .filter(it -> matches(requestBody, it.payloadType, it.matchingExpression))
                .findFirst().orElse(null);

        if (response != null) {
            log.info("{}, {}={} matchingExpression={}. Return stub answer",
                    key.queue, correlationKey, key.correlationValue, response.matchingExpression);
        }

        return response;
    }

    private boolean matches(String requestBody, PayloadType payloadType, String matchingExpression) {
        if (StringUtils.isBlank(matchingExpression)) {
            return true;
        }

        return switch (payloadType) {
            case JSON -> jsonMatches(requestBody, matchingExpression);
            case XML -> xmlMatches(requestBody, matchingExpression);
        };
    }

    private boolean xmlMatches(String xml, String xmlPath) {
        try {
            var builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            builderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            var builder = builderFactory.newDocumentBuilder();
            var xmlDocument = builder.parse(new InputSource(new StringReader(xml)));

            var xPath = XPathFactory.newInstance().newXPath();
            var nodeList = (NodeList) xPath.compile(xmlPath).evaluate(xmlDocument, XPathConstants.NODESET);
            return nodeList.getLength() > 0;
        } catch (Exception ex) {
            log.warn("XPath matching error: ", ex);
            return false;
        }
    }

    private boolean jsonMatches(String json, String jsonPath) {
        var configuration = Configuration.builder()
                .options(Option.ALWAYS_RETURN_LIST, Option.SUPPRESS_EXCEPTIONS)
                .build();

        var context = JsonPath.parse(json, configuration);
        var results = context.read(jsonPath, List.class);
        return !results.isEmpty();
    }

    public List<String> getReceivedRequests(String correlationValue, String queue) {
        return requestMap.computeIfAbsent(new Key(queue, correlationValue), k -> new ArrayList<>());
    }

    public void clearResponses(String queue, String correlationValue) {
        log.info("Clearing responses from queue={}, {}={}", queue, correlationKey, correlationValue);
        var key = new Key(queue, correlationValue);
        responseMap.computeIfPresent(key, (k, v) -> new ArrayList<>());
    }

    record Response(
            String correlationValue,
            String queue,
            String responseBody,
            PayloadType payloadType,
            String matchingExpression
    ) {
    }

    record Key(
            String queue,
            String correlationValue
    ) {
    }

    public enum PayloadType {
        JSON, XML
    }
}
