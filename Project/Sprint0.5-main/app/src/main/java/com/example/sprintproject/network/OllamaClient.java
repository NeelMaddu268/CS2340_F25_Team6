package com.example.sprintproject.network;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

public class OllamaClient {

    public interface ChatCallback {
        void onSuccess(String reply);
        void onError(String error);
    }

    public interface StreamCallback {
        void onToken(String token);         // called as tokens arrive
        void onComplete(String fullReply);  // called once at end
        void onError(String error);
    }

    private static final String TAG = "OllamaClient";

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;

    private final String baseUrl = "http://10.0.2.2:11434/api/chat";
    private final String modelName; // e.g. llama3.2

    private volatile Call activeCall; // for cancel()

    public OllamaClient() {
        this("http://10.0.2.2:11434/api/chat", "llama3.2");
    }

    public OllamaClient(String baseUrl, String modelName) {
        this.modelName = modelName;

        // generous timeouts for LLMs
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)   // 0 = no timeout for streaming
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /** Cancel the currently running call (if any). */
    public void cancelActive() {
        Call c = activeCall;
        if (c != null && !c.isCanceled()) {
            c.cancel();
        }
    }

    /**
     * Non-streaming chat.
     * messages must be an array of:
     *  { "role": "user"/"assistant"/"system", "content": "..." }
     */
    public void chat(@NonNull JSONArray messages, @NonNull ChatCallback cb) {
        JSONObject payload = buildPayload(messages, false);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .build();

        activeCall = client.newCall(request);
        activeCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        cb.onError("HTTP " + response.code());
                        return;
                    }

                    String responseText = safeBodyString(response);
                    if (responseText == null) {
                        cb.onError("Empty response from Ollama.");
                        return;
                    }

                    String reply = parseNonStreamReply(responseText);
                    if (reply == null || reply.trim().isEmpty()) {
                        cb.onError("Null/empty AI reply.");
                        return;
                    }

                    cb.onSuccess(reply);

                } catch (Exception e) {
                    cb.onError("Parse error: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * Streaming chat for real-time token updates.
     * messages format same as chat().
     */
    public void chatStream(@NonNull JSONArray messages, @NonNull StreamCallback cb) {
        JSONObject payload = buildPayload(messages, true);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .build();

        activeCall = client.newCall(request);

        activeCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                cb.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                BufferedSource source = null;
                StringBuilder full = new StringBuilder();

                try {
                    if (!response.isSuccessful()) {
                        cb.onError("HTTP " + response.code());
                        return;
                    }

                    if (response.body() == null) {
                        cb.onError("Empty stream body.");
                        return;
                    }

                    source = response.body().source();

                    while (!source.exhausted()) {
                        String line = source.readUtf8Line();
                        if (line == null || line.trim().isEmpty()) continue;

                        JSONObject chunk;
                        try {
                            chunk = new JSONObject(line);
                        } catch (JSONException je) {
                            // Sometimes Ollama emits blank/partial lines, ignore safely.
                            Log.w(TAG, "Skipping bad JSON chunk: " + line);
                            continue;
                        }

                        boolean done = chunk.optBoolean("done", false);
                        if (done) break;

                        JSONObject msgObj = chunk.optJSONObject("message");
                        if (msgObj == null) continue;

                        String token = msgObj.optString("content", "");
                        if (!token.isEmpty()) {
                            full.append(token);
                            cb.onToken(token);
                        }
                    }

                    String finalText = full.toString().trim();
                    if (finalText.isEmpty()) {
                        cb.onError("Stream ended with empty reply.");
                        return;
                    }

                    cb.onComplete(finalText);

                } catch (IOException ioe) {
                    if (call.isCanceled()) {
                        cb.onError("Canceled");
                    } else {
                        cb.onError("Stream error: " + ioe.getMessage());
                    }
                } catch (Exception e) {
                    cb.onError("Stream parse error: " + e.getMessage());
                } finally {
                    response.close();
                    if (source != null) {
                        try { source.close(); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    // -------------------- helpers --------------------

    private JSONObject buildPayload(JSONArray messages, boolean stream) {
        JSONObject json = new JSONObject();
        try {
            json.put("model", modelName);
            json.put("stream", stream);
            json.put("messages", messages);
        } catch (JSONException ignored) {

        }
        return json;
    }

    private String safeBodyString(Response response) throws IOException {
        if (response.body() == null) return null;
        return response.body().string();
    }

    /**
     * Expected non-stream response:
     * {
     *   "message": { "role": "assistant", "content": "..." },
     *   ...
     * }
     */
    private String parseNonStreamReply(String responseText) {
        try {
            JSONObject root = new JSONObject(responseText);
            JSONObject messageObj = root.optJSONObject("message");
            if (messageObj == null) return null;
            return messageObj.optString("content", null);
        } catch (JSONException e) {
            return null;
        }
    }
}
