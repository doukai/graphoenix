package io.graphoenix.http.context;

import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.*;
import reactor.netty.http.server.HttpServerResponse;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.NewCookie.DEFAULT_MAX_AGE;

public class HttpResponseContext implements ContainerResponseContext {

    private final HttpServerResponse httpServerResponse;

    public HttpResponseContext(HttpServerResponse httpServerResponse) {
        this.httpServerResponse = httpServerResponse;
    }

    @Override
    public int getStatus() {
        return httpServerResponse.status().code();
    }

    @Override
    public void setStatus(int code) {
        httpServerResponse.status(code);
    }

    @Override
    public Response.StatusType getStatusInfo() {
        return Response.Status.fromStatusCode(httpServerResponse.status().code());
    }

    @Override
    public void setStatusInfo(Response.StatusType statusInfo) {
        httpServerResponse.status(statusInfo.getStatusCode());
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return httpServerResponse.responseHeaders().entries().stream()
                .collect(MultivaluedHashMap::new,
                        (multimap, entry) -> multimap.add(entry.getKey(), entry.getValue()),
                        MultivaluedMap::putAll);
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return httpServerResponse.responseHeaders().entries().stream()
                .collect(MultivaluedHashMap::new,
                        (multimap, entry) -> multimap.add(entry.getKey(), entry.getValue()),
                        MultivaluedMap::putAll);
    }

    @Override
    public String getHeaderString(String name) {
        return httpServerResponse.responseHeaders().get(name);
    }

    @Override
    public Set<String> getAllowedMethods() {
        return null;
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
    public Map<String, NewCookie> getCookies() {
        return httpServerResponse.cookies().entrySet().stream()
                .collect(
                        Collectors.toMap(
                                entry -> (String) entry.getKey(),
                                entry -> new NewCookie.Builder((String) entry.getKey())
                                        .domain(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::domain).findFirst().orElse(null))
                                        .path(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::path).findFirst().orElse(null))
                                        .value(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::value).findFirst().orElse(null))
                                        .maxAge(entry.getValue().stream().map(cookie -> (int) cookie.maxAge()).findFirst().orElse(DEFAULT_MAX_AGE))
                                        .httpOnly(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::isHttpOnly).findFirst().orElse(false))
                                        .secure(entry.getValue().stream().map(io.netty.handler.codec.http.cookie.Cookie::isSecure).findFirst().orElse(false))
                                        .build()
                        )
                );
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public URI getLocation() {
        return null;
    }

    @Override
    public Set<Link> getLinks() {
        return null;
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public Class<?> getEntityClass() {
        return null;
    }

    @Override
    public Type getEntityType() {
        return null;
    }

    @Override
    public void setEntity(Object entity) {

    }

    @Override
    public void setEntity(Object entity, Annotation[] annotations, MediaType mediaType) {

    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return new Annotation[0];
    }

    @Override
    public OutputStream getEntityStream() {
        return null;
    }

    @Override
    public void setEntityStream(OutputStream outputStream) {

    }
}
