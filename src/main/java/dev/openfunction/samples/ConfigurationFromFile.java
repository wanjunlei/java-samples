package dev.openfunction.samples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.openfunction.functions.HttpFunction;
import dev.openfunction.functions.HttpRequest;
import dev.openfunction.functions.HttpResponse;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class ConfigurationFromFile implements HttpFunction {
    private static final String ConfigFilePathENV = "CONFIG_FILE";
    private static final String DefaultConfigFile = "/etc/openfunction/config.yaml";

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final File file;
    private Map<String, String> configs;
    private Error error;

    public ConfigurationFromFile() {
        String configFile = System.getenv(ConfigFilePathENV);
        if (configFile == null || configFile.length() == 0) {
            configFile = DefaultConfigFile;
        }

        file = new File(configFile);

        loadConfig();

        startFileWatcher();
    }

    private void startFileWatcher() {
        FileAlterationObserver observer = new FileAlterationObserver(file.getParent());
        FileAlterationMonitor monitor = new FileAlterationMonitor(1000);
        monitor.addObserver(observer);
        observer.addListener(new FileListener(file));
        try {
            monitor.start();
        } catch (Exception e) {
            System.out.println("start file monitor failed");
            e.printStackTrace();
            error = new Error(e.getMessage());
        }
    }

    synchronized private void loadConfig() {
        try {
            configs = new Yaml().load(new FileReader(file));
            System.out.println("load config");
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

        if (configs == null) {
            response.setStatusCode(200);
            return;
        }

        response.getWriter().write(GSON.toJson(configs));
    }

    class FileListener extends FileAlterationListenerAdaptor {
        private final File file;

        public FileListener(File file) {
            this.file = file;
        }

        @Override
        public void onFileCreate(File file) {
            if (this.file.getAbsolutePath().equals(file.getAbsolutePath())) {
                ConfigurationFromFile.this.loadConfig();
            }
        }

        @Override
        public void onFileChange(File file) {
            if (this.file.getAbsolutePath().equals(file.getAbsolutePath())) {
                ConfigurationFromFile.this.loadConfig();
            }
        }
    }
}
