package org.acme;


import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Provider
public class RequestTimerFilter implements ContainerRequestFilter, ContainerResponseFilter {



    private  final  MeterRegistry registry;

    private static final String START_TIME_PROPERTY = "requestStartTime";
    private static final String REQUEST_ID = "requestId";

    public RequestTimerFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = requestContext.getHeaderString("X-Request-ID");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        requestContext.setProperty(REQUEST_ID, requestId);
        requestContext.setProperty(START_TIME_PROPERTY, Instant.now());
    }



    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        Instant startTime = (Instant) requestContext.getProperty(START_TIME_PROPERTY);
        String requestId = (String) requestContext.getProperty(REQUEST_ID);

        if (startTime != null) {
            System.out.println(startTime);
            Duration duration = Duration.between(startTime, Instant.now());
            long durationMillis = duration.toMillis();

            // Basic Informations
            String path = requestContext.getUriInfo().getPath();
            String method = requestContext.getMethod();
            int status = responseContext.getStatus();

            // Request metadata
            Map<String, Object> metadata = new HashMap<>();

            // Query string
            String queryString = requestContext.getUriInfo().getRequestUri().getQuery();
            metadata.put("queryString", queryString);

            // Headers request
            metadata.put("userAgent", requestContext.getHeaderString("User-Agent"));
            metadata.put("contentType", requestContext.getHeaderString("Content-Type"));
            metadata.put("acceptLanguage", requestContext.getHeaderString("Accept-Language"));
            metadata.put("referer", requestContext.getHeaderString("Referer"));
            metadata.put("origin", requestContext.getHeaderString("Origin"));

            // Response metadata
            metadata.put("responseType", responseContext.getMediaType() != null ?
                    responseContext.getMediaType().toString() : "unknown");
            metadata.put("responseLength", responseContext.getLength());

            // Info about security
            SecurityContext secCtx = requestContext.getSecurityContext();
            if (secCtx != null) {
                metadata.put("authScheme", secCtx.getAuthenticationScheme());
                metadata.put("isSecure", secCtx.isSecure());
                if (secCtx.getUserPrincipal() != null) {
                    metadata.put("username", secCtx.getUserPrincipal().getName());
                }
            }

           //Info about errors
            if (status >= 400) {
                Object entity = responseContext.getEntity();
                if (entity != null) {
                    metadata.put("errorType", entity.getClass().getName());
                }
            }

            metadata.put("rateLimitRemaining", responseContext.getHeaderString("X-RateLimit-Remaining"));
            metadata.put("rateLimitLimit", responseContext.getHeaderString("X-RateLimit-Limit"));
            metadata.put("clientApp", requestContext.getHeaderString("X-Client-App"));
            metadata.put("apiVersion", requestContext.getHeaderString("X-API-Version"));



            registry.timer("http_request_duration_seconds_custom",
                    Tags.of("path", path,
                            "method", method,
                            "request_id", requestId,
                            "status", String.valueOf(status))).record(durationMillis, TimeUnit.MILLISECONDS);

        }
    }
}

