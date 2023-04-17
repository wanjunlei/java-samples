package dev.openfunction.samples;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.openfunction.functions.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple file system can list, get, create and delete file.
 */
public class SimpleFileSystem extends Routable implements HttpFunction {
    private static final String RootDirENV = "ROOT_DIR";

    private static final Gson GSON = new GsonBuilder().create();

    private final String rootDir;

    public SimpleFileSystem() {
        rootDir = System.getenv(RootDirENV);
    }

    @Override
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) throws Exception {
        switch (httpRequest.getMethod()) {
            case Routable.METHOD_GET:
                if (httpRequest.getQueryParameters().get("name") == null) {
                    list(httpResponse);
                } else {
                    get(httpRequest, httpResponse);
                }
                break;
            case Routable.METHOD_DELETE:
                delete(httpRequest, httpResponse);
            case Routable.METHOD_POST:
                add(httpRequest, httpResponse);
                break;
        }
    }

    private void list( HttpResponse httpResponse) throws IOException {
        File file = new File(rootDir);
        httpResponse.getWriter().write(GSON.toJson(file.list()));
    }

    private void get(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        List<String> names = httpRequest.getQueryParameters().get("name");
        Map<String, String> data = new HashMap<>();

        for (String name : names) {
            File file = new File(rootDir + "/" + name);
            if (!file.exists()) {
                data.put(name, "file does not exist");
                continue;
            }
            data.put(name, FileUtils.readFileToString(file, Charset.defaultCharset()));
        }

        httpResponse.getWriter().write(GSON.toJson(data));
    }

    private void  delete(HttpRequest httpRequest, HttpResponse httpResponse) {
        List<String> names = httpRequest.getQueryParameters().get("name");
        if (names == null || names.size() == 0) {
            return;
        }

        for (String name : names) {
            File file = new File(rootDir + "/" + name);
            boolean ok = file.delete();
            if (!ok) {
                System.out.println("delete file " + name + " failed");
            }
        }

        httpResponse.setStatusCode(200);
    }

    private void add(HttpRequest httpRequest, HttpResponse httpResponse) throws IOException {
        List<String> names = httpRequest.getQueryParameters().get("name");
        if (names == null || names.size() == 0) {
            httpResponse.setStatusCode(500);
            httpResponse.getWriter().write("no file name set");
            return;
        }

        File file = new File(rootDir + "/" + names.get(0));
        if (!file.exists()) {
            boolean ok = file.createNewFile();
            if (!ok) {
                httpResponse.setStatusCode(500);
                httpResponse.getWriter().write("create file " + names.get(0) + " failed");
                return;
            }
        }
        FileUtils.writeByteArrayToFile(file, httpRequest.getInputStream().readAllBytes());
        httpResponse.setStatusCode(200);
    }

    @Override
    public String[] getMethods() {
        return new String[]{Routable.METHOD_GET, Routable.METHOD_DELETE, Routable.METHOD_POST};
    }
}
