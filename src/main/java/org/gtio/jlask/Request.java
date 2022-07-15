package org.gtio.jlask;

import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> params = new HashMap<>();
    public boolean error = false;
    // 因为 Chrome 的预加载机制，会导致在Tcp握手后很久才开始发报文，所以需要延迟读取
    public long _firstGetByteTime = 0;
    private String url;
    private String method;
    private String version;
    private String body;
    private Map<String, String> fromBody;
    private JSONObject jsonBody;
    private String urlParams;
    private ArrayList<formData> formDataBody;

    public Request(InputStream in) throws IOException {
        InputStreamReader inStream = new InputStreamReader(in, StandardCharsets.ISO_8859_1);
        BufferedReader reader = new BufferedReader(inStream);
        ArrayList<Byte> arrBytes = new ArrayList<>();
        byte[] breakLine = new byte[4];

        while (true) {
            int b = reader.read();

            if (_firstGetByteTime == 0) {
                _firstGetByteTime = System.nanoTime();
            }

            if (b == -1) {
                break;
            }

            arrBytes.add((byte) b);
            if (arrBytes.size() >= 4) {
                System.arraycopy(breakLine, 1, breakLine, 0, 3);
                breakLine[3] = (byte) b;
                if (breakLine[0] == 13 && breakLine[1] == 10 && breakLine[2] == 13 && breakLine[3] == 10) {
                    break;
                }
            }
        }

        if (arrBytes.isEmpty()) {
            this.error = true;
            return;
        }

        byte[] bytes = new byte[arrBytes.size()];
        for (int i = 0; i < arrBytes.size(); i++) {
            bytes[i] = arrBytes.get(i);
        }

        // 已读取 header
        String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\n");
        String[] firstLine = lines[0].split(" ");
        this.method = firstLine[0];
        this.urlParams = firstLine[1];
        parseUrlParams(urlParams);
        this.version = firstLine[2];
        parseHeaders(lines);
        if (method.equals("POST") || method.equals("PUT")) {
            parseBody(reader, Integer.parseInt(headers.get("Content-Length")));
        }
        //reader.close();
    }

    private void parseBody(BufferedReader reader, int contentLength) throws IOException {

        byte[] bytes = new byte[contentLength];
        for (int i = 0; i < contentLength; i++) {
            bytes[i] = (byte) reader.read();
        }

        if (headers.get("Content-Type").startsWith("multipart/form-data")) {
            body = new String(bytes, StandardCharsets.ISO_8859_1);
            structFormData();
        } else {
            body = new String(bytes, StandardCharsets.UTF_8);
            if (headers.get("Content-Type").startsWith("application/x-www-form-urlencoded")) {
                this.body = URLDecoder.decode(body, StandardCharsets.UTF_8);
                fromBody = new HashMap<>();
                for (String kv : this.body.split("&")) {
                    String[] kvArray = kv.split("=");
                    if (kvArray.length == 2)
                        fromBody.put(kvArray[0], kvArray[1]);
                }
            } else if (headers.get("Content-Type").startsWith("application/json")) {
                jsonBody = JSONObject.parseObject(body);
            }
        }

    }

    private void structFormData() {
        formDataBody = new ArrayList<>();
        String boundary = headers.get("Content-Type").split("boundary=")[1];
        String[] objBlocks = body.split("--" + boundary);
        for (String objBlock : objBlocks) {
            if (objBlock.isEmpty() || objBlock.trim().equals("--")) continue;
            String[] dataBlocks = objBlock.split("\r\n\r\n");

            String headerLines = dataBlocks[0];
            String contentLines = dataBlocks[1];

            String[] lines = headerLines.split("\n");
            formData formData = new formData();

            for (String line : lines) {
                if (line.startsWith("Content-Disposition")) {
                    String[] kv = line.replace(" ", "").split(";");
                    for (String kvLine : kv) {
                        if (kvLine.startsWith("name=")) {
                            formData.name = kvLine.split("=")[1].replace("\"", "").trim();
                        } else if (kvLine.startsWith("filename=")) {
                            formData.filename = kvLine.split("=")[1].replace("\"", "").trim();
                        }
                    }
                } else if (line.startsWith("Content-Type")) {
                    formData.contentType = line.split(":")[1].trim().trim();
                } else if (line.startsWith("Content-Transfer-Encoding")) {
                    formData.contentTransferEncoding = line.split(":")[1].trim();
                }
            }

            if (formData.contentTransferEncoding.equals("base64")) {
                formData.binaryValue = Base64.getDecoder().decode(contentLines);
            } else {
                // 是否是文件
                if (formData.filename != null) {
                    formData.binaryValue = contentLines.getBytes(StandardCharsets.ISO_8859_1);
                } else {
                    formData.value = new String(contentLines.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                }
            }

            formDataBody.add(formData);
        }
    }

    private void parseUrlParams(String url) {
        url = URLDecoder.decode(url, StandardCharsets.UTF_8);
        String[] params = url.split("\\?");
        this.url = params[0];
        if (params.length > 1) {
            String[] kvs = params[1].split("&");
            for (String kv : kvs) {
                String[] kv2 = kv.split("=");
                if (kv2.length == 2) {
                    this.params.put(kv2[0], kv2[1]);
                }
            }
        }
    }

    private void parseHeaders(String[] lines) {
        for (int i = 1; i < lines.length; i++) {
            String[] header = lines[i].split(":");
            if (header.length == 2) {
                headers.put(header[0].trim(), header[1].trim());
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public JSONObject getJsonBody() {
        return jsonBody;
    }

    public Map<String, String> getFromBody() {
        return fromBody;
    }

    public ArrayList<formData> getFormDataBody() {
        return formDataBody;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getVersion() {
        return version;
    }

    public String getUrlParams() {
        return urlParams;
    }

}
