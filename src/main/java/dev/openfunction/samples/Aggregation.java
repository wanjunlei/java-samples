package dev.openfunction.samples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Aggregation implements HttpFunction {
    private static final String FunctionsENV = "FUNCTIONS";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private String[] functions;

    public Aggregation() {
        String functionString = System.getenv(FunctionsENV);
        if (functionString == null || functionString.length() == 0) {
            System.out.println("WARNING: no functions set");
        } else {
            functions = functionString.split(",");
        }
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        if (functions == null || functions.length == 0) {
            response.setStatusCode(200);
            return;
        }

        Map<String, String> result = new HashMap<>();
        for (String function : functions) {
            Map<String, String> slice = accessFunction(function);
            if (slice != null) {
                result.putAll(slice);
            }
        }

        response.setStatusCode(200);
        response.getWriter().write(GSON.toJson(result));
    }

    private Map<String, String> accessFunction(String function) throws IOException {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(function);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                System.out.println("access function " + function + " failed, " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
                return null;
            }

            String content = EntityUtils.toString(response.getEntity(), "UTF-8");
            return GSON.getAdapter(Map.class).fromJson(content);
        }
    }
}
