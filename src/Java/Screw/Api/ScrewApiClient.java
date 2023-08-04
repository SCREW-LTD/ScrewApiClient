import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScrewApiClient {
    private final HttpClient httpClient;
    private final String apiUrl = "https://api.screwltd.com/";

    public ScrewApiClient() {
        httpClient = HttpClient.newBuilder()
                .baseUrl(URI.create(apiUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String authenticate(String login, String password) throws IOException, InterruptedException {
        Map<String, String> authData = new HashMap<>();
        authData.put("login", login);
        authData.put("password", password);

        String authJson = new ObjectMapper().writeValueAsString(authData);

        String response = postRequest("auth", authJson);
        if (response != null && !response.isEmpty()) {
            Map<String, String> jsonResponse = new ObjectMapper().readValue(response, Map.class);
            return jsonResponse.containsKey("access_key") ? jsonResponse.get("access_key") : null;
        }

        return null;
    }

    public boolean checkAuthValid(String accessKey) {
        return accessKey != null && !accessKey.isEmpty();
    }

    public String createAppKey(String accessKey) throws IOException, InterruptedException {
        String response = postRequest("create_app", "", accessKey);
        if (response != null && !response.isEmpty()) {
            Map<String, String> jsonResponse = new ObjectMapper().readValue(response, Map.class);
            return jsonResponse.containsKey("app_key") ? jsonResponse.get("app_key") : null;
        } else {
            System.out.println("Failed to create app key. Response is empty.");
            return null;
        }
    }

    public boolean authenticateApp(String appKey, String accessKey) throws IOException, InterruptedException {
        String queryParams = "?app_key=" + appKey;
        String response = postRequest("auth_app" + queryParams, "", accessKey);

        if (response != null && !response.isEmpty()) {
            Map<String, Object> jsonResponse = new ObjectMapper().readValue(response, Map.class);
            return jsonResponse.containsKey("result") && (boolean) jsonResponse.get("result");
        }
        return false;
    }

    public int checkAppKeyActivations(String appKey) throws IOException, InterruptedException {
        String queryParams = "?app_key=" + appKey;
        String response = getRequest("check_app" + queryParams);

        if (response != null && !response.isEmpty()) {
            Map<String, Object> jsonResponse = new ObjectMapper().readValue(response, Map.class);
            return jsonResponse.containsKey("activations_left") ? (int) jsonResponse.get("activations_left") : -1;
        }
        return -1;
    }

    public boolean updateAppKey(String appKey, int numActivations, String accessKey)
            throws IOException, InterruptedException {
        String queryParams = "?app_key=" + appKey + "&num_activations=" + numActivations;
        String response = postRequest("update_app_key" + queryParams, "", accessKey);

        if (response != null && !response.isEmpty()) {
            Map<String, Object> jsonResponse = new ObjectMapper().readValue(response, Map.class);
            return jsonResponse.containsKey("message") && jsonResponse.get("message").toString().equals("App key activations updated successfully");
        }
        return false;
    }

    private String postRequest(String endpoint, String data, String accessToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Authorization", accessToken != null ? "Bearer " + accessToken : "")
                .POST(HttpRequest.BodyPublishers.ofString(data, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() == 200) {
            return httpResponse.body();
        }

        return null;
    }

    private String getRequest(String endpoint) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + endpoint))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() == 200) {
            return httpResponse.body();
        }

        return null;
    }
}
