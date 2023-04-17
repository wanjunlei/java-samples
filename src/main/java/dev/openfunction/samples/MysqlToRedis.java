package dev.openfunction.samples;

import dev.openfunction.functions.Context;
import dev.openfunction.functions.OpenFunction;
import dev.openfunction.functions.Out;
import io.dapr.client.DaprClient;
import io.dapr.client.domain.State;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlToRedis implements OpenFunction {

    private static final String RedisStateStoreNameENV = "REDIS_STATE_STORE_NAME";

    private static final String JDBCDriverENV = "JDBC_DRIVER";
    private static final String MysqlURLENV = "MYSQL_URL";
    private static final String MysqlUserENV = "MYSQL_USER";
    private static final String MysqlPasswordENV = "MYSQL_PASSWORD";
    private static final String MysqlTableENV = "MYSQL_TABLE";
    private static final String PrimaryKeyENV = "PRIMARY_KEY";

    private String redisStateStoreName;
    private final String jdbcDriver;
    private final String mysqlURL;
    private final String mysqlUser;
    private final String mysqlPassword;
    private final String mysqlTable;
    private final String primaryKey;

    public MysqlToRedis() {
        String val1 = System.getenv(RedisStateStoreNameENV);
        if (val1 == null || val1.length() == 0) {
            System.out.println("WARNING: no redis stat store set");
        } else {
            redisStateStoreName = val1;
        }

        jdbcDriver = System.getenv(JDBCDriverENV);
        mysqlURL = System.getenv(MysqlURLENV);
        mysqlUser = System.getenv(MysqlUserENV);
        mysqlPassword = System.getenv(MysqlPasswordENV);
        mysqlTable = System.getenv(MysqlTableENV);
        primaryKey = System.getenv(PrimaryKeyENV);
    }

    @Override
    public Out accept(Context context, String payload) throws Exception {
        DaprClient daprClient = context.getDaprClient();
        if (context.getDaprClient() == null) {
            return new Out().setError(new Error("dapr client is null"));
        }

        Map<String, Object> states = readFromMysql();
        if (states.isEmpty()) {
            return null;
        }

        writeToRedis(daprClient, states);

        return null;
    }

    private Map<String, Object> readFromMysql() throws Exception {
        Class.forName(jdbcDriver);
        Connection conn = DriverManager.getConnection(mysqlURL, mysqlUser, mysqlPassword);
        Statement stmt = conn.createStatement();

        String sql = "SELECT * FROM " + mysqlTable;
        ResultSet rs = stmt.executeQuery(sql);

        ResultSetMetaData metadata = rs.getMetaData();

        Map<String, Object> results = new HashMap<>();
        while (rs.next()) {
            Map<String, String> line = new HashMap<>();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                String name = metadata.getColumnLabel(i);
                line.put(name, rs.getString(name));
            }
            results.put(rs.getString(primaryKey), line);
        }

        stmt.close();
        conn.close();

        return results;
    }

    private void writeToRedis(DaprClient daprClient, Map<String, Object> states) {
        List<State<?>> stateList = new ArrayList<>();
        for (String key : states.keySet()) {
            stateList.add(new State<>(key, states.get(key), ""));
        }

        daprClient.saveBulkState(redisStateStoreName, stateList).block();
        System.out.println("save " + stateList.size() + " states");
    }
}
