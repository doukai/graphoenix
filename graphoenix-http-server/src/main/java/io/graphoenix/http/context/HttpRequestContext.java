package io.graphoenix.http.context;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;
import reactor.netty.http.server.HttpServerRequest;

import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class HttpRequestContext implements ContainerRequestContext {

    private final HttpServerRequest httpServerRequest;
    private final Map<String, Object> properties;

    public HttpRequestContext(HttpServerRequest httpServerRequest, Map<String, Object> properties) {
        this.httpServerRequest = httpServerRequest;
        this.properties = properties;
        if (httpServerRequest.params() != null) {
            this.properties.putAll(Objects.requireNonNull(httpServerRequest.params()));
        }
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public void setProperty(String name, Object object) {
        properties.put(name, object);
    }

    @Override
    public void removeProperty(String name) {
        properties.remove(name);
    }

    @Override
    public UriInfo getUriInfo() {
        return null;
    }

    @Override
    public void setRequestUri(URI requestUri) {
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) {

    }

    @Override
    public Request getRequest() {
        return null;
    }

    @Override
    public String getMethod() {
        return httpServerRequest.method().name();
    }

    @Override
    public void setMethod(String method) {
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return httpServerRequest.requestHeaders().entries().stream()
                .collect(MultivaluedHashMap::new,
                        (multimap, entry) -> multimap.add(entry.getKey(), entry.getValue()),
                        MultivaluedMap::putAll);
    }

    @Override
    public String getHeaderString(String name) {
        return httpServerRequest.requestHeaders().get(name);
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return null;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return null;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return httpServerRequest.cookies().entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> (String) entry.getKey(),
                                entry -> new Cookie.Builder((String) entry.getKey())
                                        .domain(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::domain).findFirst().orElse(null))
                                        .path(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::path).findFirst().orElse(null))
                                        .value(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::value).findFirst().orElse(null))
                                        .build()
                        )
                );
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public InputStream getEntityStream() {
        return null;
    }

    @Override
    public void setEntityStream(InputStream input) {

    }

    @Override
    public SecurityContext getSecurityContext() {
        return null;
    }

    @Override
    public void setSecurityContext(SecurityContext context) {

    }

    @Override
    public void abortWith(Response response) {

    }
}
