package dev.openfunction.samples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.openfunction.functions.*;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StateStore extends Routable implements OpenFunction {
    private static final String StateStoreNameENV = "STATE_STORE_NAME";

    private static final Gson GSON = new GsonBuilder().create();

    private  String stateStoreName;

    public StateStore() {
        String val = System.getenv(StateStoreNameENV);
        if (val == null || val.length() == 0) {
            System.out.println("WARNING: no state store set");
        } else {
            stateStoreName = val;
        }
    }

    @Override
    public String[] getMethods() {
        return new String[]{Routable.METHOD_GET, Routable.METHOD_DELETE, Routable.METHOD_POST};
    }

    @Override
    public Out accept(Context context, String payload) throws Exception {
        DaprClient daprClient = context.getDaprClient();
        if (context.getDaprClient() == null) {
            return new Out().setError(new Error("dapr client is null"));
        }

        switch (context.getHttpRequest().getMethod().toUpperCase()) {
            case Routable.METHOD_GET:
                return get(daprClient, context.getHttpRequest().getQueryParameters().get("key"));
            case Routable.METHOD_POST:
                return save(daprClient, payload);
            case Routable.METHOD_DELETE:
                return delete(daprClient, context.getHttpRequest().getQueryParameters().get("key"));
            default:
                return null;
        }
    }

    public Out get(DaprClient daprClient, List<String> keys) {
        if (keys == null || keys.size() == 0) {
            return new Out().setData(ByteBuffer.wrap(("get 0 states").getBytes()));
        }

        List<State<Object>> states = daprClient.getBulkState(stateStoreName, keys, Object.class).block();
        return new Out().setData(ByteBuffer.wrap(GSON.toJson(states).getBytes()));
    }

    public Out save(DaprClient daprClient, String payload) throws Exception {
        Map<String, String> states = GSON.getAdapter(Map.class).fromJson(payload);
        if (states == null || states.isEmpty()) {
            return new Out().setData(ByteBuffer.wrap(("save 0 states").getBytes()));
        }

        List<State<?>> stateList = new ArrayList<>();
        for (String key : states.keySet()) {
            stateList.add(new State<>(key, states.get(key), ""));
        }

        daprClient.saveBulkState(stateStoreName, stateList).block();

        return new Out().setData(ByteBuffer.wrap(("save " + stateList.size() + " states").getBytes()));
    }

    public Out delete(DaprClient daprClient, List<String> keys) {
        if (keys == null || keys.size() == 0) {
            return new Out().setData(ByteBuffer.wrap(("delete 0 states").getBytes()));
        }

        for (String key : keys) {
            daprClient.deleteState(stateStoreName, key).block();
        }

        return new Out().setData(ByteBuffer.wrap(("delete " + keys.size() + " states").getBytes()));
    }
}
