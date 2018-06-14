package com.yeshj.soa.reservation.api.common.springbootactuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.TraceProperties;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.actuate.trace.WebRequestTraceFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Include request and response body for spring boot actuator
 *
 * @author wufanyang@hujiang.com
 * @date 2018/6/4 10:44
 **/
@Component
public class RequestTraceFilter extends WebRequestTraceFilter {
    private static final String RESPONSE_BODY = "resBody";
    private static final String REQUEST_BODY = "reqBody";
    private static final String STATUS_CODE = "statusCode";
    private TraceRepository repositoryCopy;

    @Autowired
    public RequestTraceFilter(TraceRepository repository, TraceProperties properties) {
        super(repository, properties);
        repositoryCopy = repository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
//        String jsonBody =  org.apache.commons.io.IOUtils.toString(request.getInputStream());
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

//        super.doFilterInternal(request, response, filterChain);
//        responseWrapper.copyBodyToResponse();

//        int status = HttpStatus.INTERNAL_SERVER_ERROR.value();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
            request.setAttribute(REQUEST_BODY, getRequestBody(requestWrapper));
            request.setAttribute(RESPONSE_BODY, getResponseBody(responseWrapper));
            request.setAttribute(STATUS_CODE, responseWrapper.getStatusCode());
            responseWrapper.copyBodyToResponse();

//            status = response.getStatus();
        } finally {
            Map<String, Object> trace = this.getTrace(request);
//            this.enhanceTrace(trace, (HttpServletResponse)(status == response.getStatus() ? response : new WebRequestTraceFilter.CustomStatusResponseWrapper(response, status)));
            repositoryCopy.add(trace);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        return getPayload(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    @Override
    protected Map<String, Object> getTrace(HttpServletRequest request) {
        Map<String, Object> trace = super.getTrace(request);
        Object requestBody = request.getAttribute(REQUEST_BODY);
        Object responseBody = request.getAttribute(RESPONSE_BODY);
        Object statusCode = request.getAttribute(STATUS_CODE);
        if (requestBody != null) {
            trace.put(REQUEST_BODY, (String) requestBody);
        }
        if (responseBody != null) {
            trace.put(RESPONSE_BODY, (String) responseBody);
        }
        if (statusCode != null) {
            trace.put(STATUS_CODE, statusCode.toString());
        }
        return trace;
    }

    @Override
    protected void enhanceTrace(Map<String, Object> trace, HttpServletResponse response) {
        super.enhanceTrace(trace, response);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        trace.put(RESPONSE_BODY, getResponseBody(responseWrapper));
        trace.put(STATUS_CODE, responseWrapper.getStatusCode());
    }

    private String getPayload(byte[] buf, String characterEncoding) {
        String payload = "";
        if (buf.length > 0) {
            try {
                payload = new String(buf, 0, buf.length, characterEncoding);
            } catch (UnsupportedEncodingException ex) {
                payload = "[unknown]";
            }
        }
        return payload;
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(
                response, ContentCachingResponseWrapper.class);
        return unzip(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    // The content is zipped by HJ framework, need to unzip to get the original string.
    private String unzip(byte[] zippedContent, String characterEncoding) {
        if (zippedContent.length == 0) {
            return "";
        }

        try (
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(zippedContent);
                GZIPInputStream ungzippedResponse = new GZIPInputStream(byteArrayInputStream)) {
            byte[] buffer = new byte[1024];
            StringBuilder result = new StringBuilder();
            try {
                while ((ungzippedResponse.read(buffer)) > 0) {
                    result.append(new String(buffer, characterEncoding));
                    Arrays.fill(buffer, (byte) 0);
                }
                return result.toString().trim();
            } catch (EOFException e) {
                //ignore
                return result.toString().trim();
            }
        } catch (IOException e) {
            return getPayload(zippedContent, characterEncoding);
        }
    }

}
