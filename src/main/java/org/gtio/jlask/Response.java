package org.gtio.jlask;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Response {
    private final Map<String, String> headers = new HashMap<>();
    public boolean isBinarybody = false;
    private int status = 200;
    private String body;
    private byte[] binaryBody;

    public Response(byte[] body, String contentType, boolean isAttachmen, String filename) {
        this.binaryBody = body;
        this.isBinarybody = true;
        headers.put("Content-Length", body.length + "");
        headers.put("Content-Type", contentType);
        headers.put("server", "Jlask/1.0 (Java)");
        headers.put("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
    }

    public Response(byte[] body, String contentType) {
        this.binaryBody = body;
        this.isBinarybody = true;
        headers.put("Content-Length", body.length + "");
        headers.put("Content-Type", contentType);
        headers.put("server", "Jlask/1.0 (Java)");
    }

    public Response(String body, Map<String, String> headers) {
        this.body = body;
        headers.put("Content-Length", body.getBytes(StandardCharsets.UTF_8).length + "");
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "text/html; charset=UTF-8");
        }
        headers.put("server", "Jlask/1.0 (Java)");
        this.headers.putAll(headers);
    }

    public Response(String body, String contentType) {
        this.body = body;
        headers.put("Content-Length", body.getBytes(StandardCharsets.UTF_8).length + "");
        headers.put("Content-Type", contentType);
        headers.put("server", "Jlask/1.0 (Java)");
    }

    public Response(String body) {
        this.body = body;
        headers.put("Content-Length", body.getBytes(StandardCharsets.UTF_8).length + "");
        headers.put("Content-Type", "text/html; charset=UTF-8");
        headers.put("server", "Jlask/1.0 (Java)");
    }

    public static Response Redirect(String url) {
        Response response = new Response("");
        response.status = 302;
        response.headers.put("Location", url);
        return response;
    }

    private static Response NotFound() {
        String body = "<html><head><title>404 Not Found</title></head><body><h1>Not Found</h1><p>The requested URL was not found on the server. If you entered the URL manually please check your spelling and try again.</p></body></html>";
        Response response = new Response(body);
        response.status = 404;
        return response;
    }

    private static Response Forbidden() {
        String body = "<html><head><title>403 Forbidden</title></head><body><h1>Forbidden</h1><p>You don't have permission to access this page. Please contact the server administrator if you think this is a server error.</p></body></html>";
        Response response = new Response(body);
        response.status = 403;
        return response;
    }

    private static Response InternalServerError() {
        String body = "<html><head><title>500 Internal Server Error</title></head><body><h1>Internal Server Error</h1><p>The server encountered an internal error and was unable to complete your request. Please contact the server administrator if you think this is a server error.</p></body></html>";
        Response response = new Response(body);
        response.status = 500;
        return response;
    }

    private static Response NotImplemented() {
        String body = "<html><head><title>501 Not Implemented</title></head><body><h1>Not Implemented</h1><p>The server does not support the functionality required to fulfill your request. Please contact the server administrator if you think this is a server error.</p></body></html>";
        Response response = new Response(body);
        response.status = 501;
        return response;
    }

    private static Response BadRequest() {
        String body = "<html><head><title>400 Bad Request</title></head><body><h1>Bad Request</h1><p>The request was invalid. Please contact the server administrator if you think this is a server error.</p></body></html>";
        Response response = new Response(body);
        response.status = 400;
        return response;
    }

    public static Response ErrorStatus(ErrorType status) {
        switch (status) {
            case Err_400:
                return BadRequest();
            case Err_403:
                return Forbidden();
            case Err_404:
                return NotFound();
            case Err_501:
                return NotImplemented();
            case Err_500:
            default:
                return InternalServerError();
        }
    }

    public static Response ErrorStatus(int status, String body) {
        Response response = new Response(body);
        response.status = status;
        return response;
    }

    public String getVersion() {
        return "HTTP/1.1 ";
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public byte[] getBinaryBody() {
        return binaryBody;
    }

    public void SetCookie(String key, String value) {
        headers.put("Set-Cookie", key + "=" + value);
    }
}
