package dev.openfunction.samples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

public class ConfigurationFromConfigmap implements HttpFunction {
    private static final String ConfigMapENV = "CONFIGMAP_NAME";
    private static final String NamespaceENV = "CONFIGMAP_NAMESPACE";
    private static final String ConfigMapKeyENV = "CONFIGMAP_KEY";
    private static final String DefaultConfigMap = "function-config";
    private static final String DefaultNamespace = "default";
    private static final String DefaultConfigMapKey = "config.yaml";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final String configMapName;
    private final String configMapNamespace;
    private final String configMapkey;
    private SharedIndexInformer<V1ConfigMap> informer;
    private Error error;

    public static void main(String[] args) {
        new ConfigurationFromConfigmap();
    }

    public ConfigurationFromConfigmap() {
        String name = System.getenv(ConfigMapENV);
        if (name == null || name.length() == 0) {
            name = DefaultConfigMap;
        }

        String namespace = System.getenv(NamespaceENV);
        if (namespace == null || namespace.length() == 0) {
            namespace = DefaultNamespace;
        }

        String key = System.getenv(ConfigMapKeyENV);
        if (key == null || key.length() == 0) {
            key = DefaultConfigMapKey;
        }

        configMapName = name;
        configMapNamespace = namespace;
        configMapkey = key;

        startK8sInfomer();
    }

    private void startK8sInfomer() {
        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            SharedInformerFactory factory = new SharedInformerFactory(client);

            CoreV1Api coreV1Api = new CoreV1Api();
            informer = factory.sharedIndexInformerFor(
                    (CallGeneratorParams params) -> {
                        return coreV1Api.listNamespacedConfigMapCall(
                                configMapNamespace,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                params.watch,
                                null);
                    },
                    V1ConfigMap.class,
                    V1ConfigMapList.class);

            factory.startAllRegisteredInformers();
        } catch (Exception e) {
            e.printStackTrace();
            error = new Error(e.getMessage());
        }
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws Exception {
        if (error != null) {
            response.setStatusCode(500);
            response.getWriter().write(error.getMessage());
            return;
        }

        V1ConfigMap cm = new Lister<>(informer.getIndexer()).namespace(configMapNamespace).get(configMapName);
        if (cm == null) {
            response.setStatusCode(500);
            response.getWriter().write("configmap " + configMapName + "/" + configMapNamespace + " does not exist");
            return;
        }

        if (cm.getData() == null || cm.getData().get(configMapkey) == null) {
            response.setStatusCode(200);
            return;
        }

        try {
            Map<String, String> configs = new Yaml().load(cm.getData().get(configMapkey));
            response.getWriter().write(GSON.toJson(configs));
        } catch (Exception e) {
            response.setStatusCode(500);
            response.getWriter().write(e.getMessage());
        }
    }
}
