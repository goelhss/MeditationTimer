package com.vishalgoel.meditationtimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DriveAppDataClient {
    private static final String FILE_NAME = "meditation-timer-backup.json";
    private static final String DRIVE_API = "https://www.googleapis.com/drive/v3/files";
    private static final String DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3/files";
    private static final int TIMEOUT_MS = 20_000;

    public void upload(String accessToken, String backupJson) throws IOException {
        requireToken(accessToken);
        List<String> existingIds = findBackupIds(accessToken);
        if (existingIds.isEmpty()) {
            create(accessToken, backupJson);
        } else {
            update(accessToken, existingIds.get(0), backupJson);
            for (int index = 1; index < existingIds.size(); index++) {
                deleteById(accessToken, existingIds.get(index));
            }
        }
    }

    public String download(String accessToken) throws IOException {
        requireToken(accessToken);
        String id = findNewestBackupId(accessToken);
        if (id == null) {
            throw new IOException("No Meditation Timer backup was found in Google Drive.");
        }
        String encodedId = urlEncode(id);
        HttpURLConnection connection = open(DRIVE_API + "/" + encodedId + "?alt=media",
                "GET", accessToken);
        return executeForText(connection, BackupCodec.MAX_BACKUP_BYTES);
    }

    public boolean hasBackup(String accessToken) throws IOException {
        requireToken(accessToken);
        return findNewestBackupId(accessToken) != null;
    }

    public int deleteBackup(String accessToken) throws IOException {
        requireToken(accessToken);
        List<String> ids = findBackupIds(accessToken);
        for (String id : ids) {
            deleteById(accessToken, id);
        }
        return ids.size();
    }

    private String findNewestBackupId(String token) throws IOException {
        List<String> ids = findBackupIds(token);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private List<String> findBackupIds(String token) throws IOException {
        String query = "name='" + FILE_NAME + "' and trashed=false";
        String url = DRIVE_API
                + "?spaces=appDataFolder&pageSize=100&orderBy=modifiedTime%20desc"
                + "&fields=files(id,name,modifiedTime)&q="
                + urlEncode(query);
        HttpURLConnection connection = open(url, "GET", token);
        String response = executeForText(connection, 256 * 1024);
        try {
            JSONArray files = new JSONObject(response).optJSONArray("files");
            if (files == null || files.length() == 0) {
                return List.of();
            }
            List<String> ids = new ArrayList<>();
            for (int index = 0; index < files.length(); index++) {
                String id = files.getJSONObject(index).optString("id", "");
                if (!id.isBlank()) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (JSONException error) {
            throw new IOException("Google Drive returned an invalid file list.", error);
        }
    }

    private void deleteById(String token, String id) throws IOException {
        String encodedId = urlEncode(id);
        HttpURLConnection connection = open(DRIVE_API + "/" + encodedId,
                "DELETE", token);
        executeForText(connection, 64 * 1024);
    }

    private void create(String token, String json) throws IOException {
        String boundary = "MeditationTimer-" + UUID.randomUUID();
        JSONObject metadata = new JSONObject();
        try {
            metadata.put("name", FILE_NAME);
            metadata.put("parents", new JSONArray().put("appDataFolder"));
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeUtf8(payload, "--" + boundary + "\r\n");
        writeUtf8(payload, "Content-Type: application/json; charset=UTF-8\r\n\r\n");
        writeUtf8(payload, metadata.toString());
        writeUtf8(payload, "\r\n--" + boundary + "\r\n");
        writeUtf8(payload, "Content-Type: application/json; charset=UTF-8\r\n\r\n");
        writeUtf8(payload, json);
        writeUtf8(payload, "\r\n--" + boundary + "--\r\n");

        HttpURLConnection connection = open(DRIVE_UPLOAD + "?uploadType=multipart&fields=id",
                "POST", token);
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
        send(connection, payload.toByteArray());
        executeForText(connection, 256 * 1024);
    }

    private void update(String token, String id, String json) throws IOException {
        String encodedId = urlEncode(id);
        HttpURLConnection connection = open(DRIVE_UPLOAD + "/" + encodedId
                + "?uploadType=media&fields=id", "PATCH", token);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        send(connection, json.getBytes(StandardCharsets.UTF_8));
        executeForText(connection, 256 * 1024);
    }

    private static HttpURLConnection open(String url, String method, String token)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private static void send(HttpURLConnection connection, byte[] bytes) throws IOException {
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(bytes);
        }
    }

    private static String executeForText(HttpURLConnection connection, int maxBytes)
            throws IOException {
        int status = connection.getResponseCode();
        InputStream input = status >= 200 && status < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String body = input == null ? "" : readLimited(input, maxBytes);
        connection.disconnect();
        if (status < 200 || status >= 300) {
            if (status == 401 || status == 403) {
                throw new IOException("Google Drive access expired or was denied. Reconnect and try again.");
            }
            throw new IOException("Google Drive request failed (" + status + ").");
        }
        return body;
    }

    private static String readLimited(InputStream input, int maxBytes) throws IOException {
        try (input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8_192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) {
                    throw new IOException("Google Drive returned a file that is too large.");
                }
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String urlEncode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private static void writeUtf8(OutputStream output, String text) throws IOException {
        output.write(text.getBytes(StandardCharsets.UTF_8));
    }

    private static void requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Google did not provide an access token.");
        }
    }
}
