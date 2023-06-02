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

    private static final Gson GSON = new GsonBuilder().create();

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

        Map<String, Component> states = context.getStates();
        if (states == null) {
            return new Out().setData(ByteBuffer.wrap("no state store found".getBytes()));
        }

        // We assume that there is only one state
        for (String name : states.keySet()) {
            switch (context.getHttpRequest().getMethod().toUpperCase()) {
                case Routable.METHOD_GET:
                    return get(daprClient, states.get(name).getComponentName(), context.getHttpRequest().getQueryParameters().get("key"));
                case Routable.METHOD_POST:
                    return save(daprClient, states.get(name).getComponentName(), payload);
                case Routable.METHOD_DELETE:
                    return delete(daprClient, states.get(name).getComponentName(), context.getHttpRequest().getQueryParameters().get("key"));
                default:
                    return null;
            }
        }

        return null;
    }

    public Out get(DaprClient daprClient, String stateStoreName, List<String> keys) {
        if (keys == null || keys.size() == 0) {
            return new Out().setData(ByteBuffer.wrap(("get 0 states").getBytes()));
        }

        List<State<Object>> states = daprClient.getBulkState(stateStoreName, keys, Object.class).block();
        return new Out().setData(ByteBuffer.wrap(GSON.toJson(states).getBytes()));
    }

    public Out save(DaprClient daprClient, String stateStoreName, String payload) throws Exception {
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

    public Out delete(DaprClient daprClient, String stateStoreName, List<String> keys) {
        if (keys == null || keys.size() == 0) {
            return new Out().setData(ByteBuffer.wrap(("delete 0 states").getBytes()));
        }

        for (String key : keys) {
            daprClient.deleteState(stateStoreName, key).block();
        }

        return new Out().setData(ByteBuffer.wrap(("delete " + keys.size() + " states").getBytes()));
    }
}
