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
        void onToken(String token);
        void onComplete(String fullReply);
        void onError(String error);
    }

    private static final String TAG = "OllamaClient";

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;

    private final String baseUrl;
    private final String modelName;

    private volatile Call activeCall;

    public OllamaClient() {
        this("http://10.0.2.2:11434/api/chat", "llama3.2");
    }

    public OllamaClient(String baseUrl, String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void cancelActive() {
        Call c = activeCall;
        if (c != null && !c.isCanceled()) {
            c.cancel();
        }
    }

    public void chat(@NonNull JSONArray messages, @NonNull ChatCallback cb) {
        JSONObject payload = buildPayload(messages, false);

        Log.d(TAG, "chat payload: " + payload);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .build();

        activeCall = client.newCall(request);
        activeCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "chat onFailure", e);
                cb.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null
                                ? response.body().string() : "";
                        Log.e(TAG, "chat HTTP " + response.code() + " from Ollama: " + errBody);
                        cb.onError("HTTP " + response.code());
                        return;
                    }

                    String responseText = safeBodyString(response);
                    Log.d(TAG, "chat raw response: " + responseText);

                    if (responseText == null) {
                        cb.onError("Empty response from Ollama.");
                        return;
                    }

                    String reply = parseNonStreamReply(responseText);
                    if (reply == null || reply.trim().isEmpty()) {
                        cb.onError("Null/empty AI reply.");
                        return;
                    }

                    cb.onSuccess(reply.trim());

                } catch (Exception e) {
                    Log.e(TAG, "chat parse error", e);
                    cb.onError("Parse error: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    public void chatStream(@NonNull JSONArray messages, @NonNull StreamCallback cb) {
        JSONObject payload = buildPayload(messages, true);

        Log.d(TAG, "chatStream payload: " + payload);

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrl)
                .post(body)
                .build();

        activeCall = client.newCall(request);

        activeCall.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "chatStream onFailure", e);
                cb.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                BufferedSource source = null;
                StringBuilder full = new StringBuilder();

                try {
                    if (!response.isSuccessful()) {
                        String errBody = response.body() != null
                                ? response.body().string() : "";
                        Log.e(TAG, "chatStream HTTP " + response.code()
                                + " from Ollama: " + errBody);
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
                        if (line == null || line.trim().isEmpty()) {
                            continue;
                        }
                        Log.d(TAG, "chatStream chunk: " + line);

                        JSONObject chunk = checkChunk(line);
                        if (chunk == null) {
                            continue;
                        }
                        boolean done = chunk.optBoolean("done", false);
                        if (done) {
                            break;
                        }

                        JSONObject msgObj = chunk.optJSONObject("message");
                        if (msgObj == null) {
                            continue;
                        }

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
                    Log.e(TAG, "chatStream IOException", ioe);
                    if (call.isCanceled()) {
                        cb.onError("Canceled");
                    } else {
                        cb.onError("Stream error: " + ioe.getMessage());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "chatStream parse error", e);
                    cb.onError("Stream parse error: " + e.getMessage());
                } finally {
                    response.close();
                    if (source != null) {
                        try {
                            source.close();
                        } catch (Exception ignored) {
                            // Intentionally ignored due to Firebase limitations
                        }
                    }
                }
            }
        });
    }

    private JSONObject checkChunk(String line) {
        try {
            return new JSONObject(line);
        } catch (JSONException e) {
            Log.e(TAG, "chunk JSON error", e);
            return null;
        }
    }

    private JSONObject buildPayload(JSONArray messages, boolean stream) {
        JSONObject json = new JSONObject();
        try {
            json.put("model", modelName);
            json.put("stream", stream);
            json.put("messages", messages);
        } catch (JSONException e) {
            Log.e(TAG, "Error building payload", e);
        }
        return json;
    }

    private String safeBodyString(Response response) throws IOException {
        if (response.body() == null) return null;
        return response.body().string();
    }

    private String parseNonStreamReply(String responseText) {
        try {
            JSONObject root = new JSONObject(responseText);
            JSONObject messageObj = root.optJSONObject("message");
            if (messageObj == null) return null;
            return messageObj.optString("content", null);
        } catch (JSONException e) {
            Log.e(TAG, "parseNonStreamReply JSON error", e);
            return null;
        }
    }
}
