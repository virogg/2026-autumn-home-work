package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;

public final class UsersHandler implements HttpHandler {
    private final Dao<String> users;

    public UsersHandler(Dao<String> users) {
        this.users = users;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"/internal/users".equals(exchange.getRequestURI().getPath())) {
            HttpUtils.sendEmpty(exchange, HttpURLConnection.HTTP_NOT_FOUND);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())) {
            HttpUtils.sendEmpty(exchange, HttpURLConnection.HTTP_BAD_METHOD);
            return;
        }
        String line = HttpUtils.readBody(exchange).lines().findFirst().orElse("");
        int colon = line.indexOf(':');
        if (colon <= 0) {
            HttpUtils.sendEmpty(exchange, HttpUtils.HTTP_UNPROCESSABLE_CONTENT);
            return;
        }
        users.upsert(line.substring(0, colon), line.substring(colon + 1));
        HttpUtils.sendEmpty(exchange, HttpURLConnection.HTTP_OK);
    }
}
