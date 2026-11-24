package app.hereisswapnil.cameratv;

import java.net.URI;
import java.nio.charset.StandardCharsets;

public class CameraItem {
    public final String id;
    public final String name;
    public final String url;
    public final boolean audio;

    // Optional discrete parameters
    public final String ip;
    public final int port;
    public final String username;
    public final String password;
    public final String path;
    public final String protocol;

    public CameraItem(String id, String name, String url, boolean audio) {
        this(id, name, url, audio, null, 0, null, null, null, null);
    }

    public CameraItem(String id, String name, String url, boolean audio,
                      String ip, int port, String username, String password,
                      String path, String protocol) {
        this.id = id;
        this.name = name;
        this.audio = audio;
        this.ip = (ip != null) ? ip.trim() : "";
        this.port = (port > 0) ? port : 554;
        this.username = (username != null) ? username.trim() : "";
        this.password = (password != null) ? password.trim() : "";
        this.path = (path != null && !path.trim().isEmpty())
                ? (path.trim().startsWith("/") ? path.trim() : "/" + path.trim())
                : "/stream1";
        this.protocol = (protocol != null && !protocol.trim().isEmpty()) ? protocol.trim() : "rtsp";

        if (url != null && !url.trim().isEmpty()) {
            String raw = url.trim();
            // If url lacks credentials but username was supplied, inject them
            if (!raw.contains("@") && !this.username.isEmpty()) {
                int schemeEnd = raw.indexOf("://");
                if (schemeEnd != -1) {
                    String prefix = raw.substring(0, schemeEnd + 3);
                    String rest = raw.substring(schemeEnd + 3);
                    String creds = encodeComponent(this.username) +
                            (!this.password.isEmpty() ? ":" + encodeComponent(this.password) : "") + "@";
                    raw = prefix + creds + rest;
                }
            }
            this.url = normalizeUrl(raw);
        } else if (!this.ip.isEmpty()) {
            this.url = buildUrlFromComponents(this.protocol, this.username, this.password, this.ip, this.port, this.path);
        } else {
            this.url = "";
        }
    }

    public String getHost() {
        if (!ip.isEmpty()) return ip;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) return host;
        } catch (Exception ignored) {}
        return "";
    }

    private static String buildUrlFromComponents(String protocol, String username, String password,
                                                 String ip, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://");

        if (username != null && !username.isEmpty()) {
            sb.append(encodeComponent(username));
            if (password != null && !password.isEmpty()) {
                sb.append(":").append(encodeComponent(password));
            }
            sb.append("@");
        }

        sb.append(ip);
        if (port > 0) {
            sb.append(":").append(port);
        }

        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) sb.append("/");
            sb.append(path);
        }

        return sb.toString();
    }

    /**
     * Automatically normalizes and percent-encodes username and password
     * so special characters (like @, #, %, :) in credentials do not need
     * to be manually URL-encoded in the config JSON.
     */
    private static String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        rawUrl = rawUrl.trim();

        int schemeEnd = rawUrl.indexOf("://");
        if (schemeEnd == -1) {
            return rawUrl;
        }

        String scheme = rawUrl.substring(0, schemeEnd + 3);
        String afterScheme = rawUrl.substring(schemeEnd + 3);

        int slashIdx = afterScheme.indexOf('/');
        String authority = (slashIdx != -1) ? afterScheme.substring(0, slashIdx) : afterScheme;
        String pathAndBeyond = (slashIdx != -1) ? afterScheme.substring(slashIdx) : "";

        // The last '@' in authority separates userInfo from host[:port]
        int lastAt = authority.lastIndexOf('@');
        if (lastAt == -1) {
            return rawUrl;
        }

        String userInfo = authority.substring(0, lastAt);
        String hostPort = authority.substring(lastAt + 1);

        // In userInfo, the first ':' separates username from password
        int colonIdx = userInfo.indexOf(':');
        String encodedUserInfo;
        if (colonIdx != -1) {
            String username = userInfo.substring(0, colonIdx);
            String password = userInfo.substring(colonIdx + 1);
            encodedUserInfo = encodeComponent(username) + ":" + encodeComponent(password);
        } else {
            encodedUserInfo = encodeComponent(userInfo);
        }

        return scheme + encodedUserInfo + "@" + hostPort + pathAndBeyond;
    }

    private static String encodeComponent(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int len = input.length();
        int i = 0;
        while (i < len) {
            char c = input.charAt(i);
            // Preserve valid existing percent-encoding (e.g. %40, %23)
            if (c == '%' && i + 2 < len && isHex(input.charAt(i + 1)) && isHex(input.charAt(i + 2))) {
                sb.append(input, i, i + 3);
                i += 3;
                continue;
            }

            if (isUnreserved(c)) {
                sb.append(c);
                i++;
            } else {
                int codePoint = Character.codePointAt(input, i);
                int charCount = Character.charCount(codePoint);
                String sub = input.substring(i, i + charCount);
                byte[] bytes = sub.getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    sb.append(String.format("%%%02X", b & 0xFF));
                }
                i += charCount;
            }
        }
        return sb.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~';
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }
}
