package company.vk.edu.distrib.compute.virogg.urlshortener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import company.vk.edu.distrib.compute.urlshortener.UrlShortenerService;
import org.jspecify.annotations.Nullable;

public final class UrlShortenerServiceImpl implements UrlShortenerService {
    private final int port;
    private final Path dataDir;
    @Nullable
    private PersistentDao links;
    @Nullable
    private PersistentDao users;
    @Nullable
    private HttpServer server;
    @Nullable
    private ExecutorService executor;

    public UrlShortenerServiceImpl(int port, Path dataDir) {
        this.port = port;
        this.dataDir = dataDir;
    }

    @Override
    public synchronized void start() {
        if (server != null) {
            throw new IllegalStateException("Service has already been started");
        }

        try {
            PersistentDao linksDao = new PersistentDao(dataDir, "links.log");
            links = linksDao;
            PersistentDao usersDao = new PersistentDao(dataDir, "users.log");
            users = usersDao;
            server = createServer(linksDao, usersDao);
        } catch (IOException e) {
            closeStorage();
            throw new UncheckedIOException("Failed to start urlshortener service on port " + port, e);
        } catch (RuntimeException e) {
            closeStorage();
            throw e;
        }
    }

    @Override
    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        closeStorage();
    }

    private HttpServer createServer(PersistentDao linksDao, PersistentDao usersDao) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        LinksHandler linksHandler = new LinksHandler(linksDao, port);
        httpServer.createContext("/v0/status", HttpUtils.safe(exchange -> status(exchange, linksDao, usersDao)));
        httpServer.createContext("/v0/links", HttpUtils.safe(linksHandler))
            .setAuthenticator(new UsersAuthenticator(usersDao));
        httpServer.createContext("/internal/users", HttpUtils.safe(new UsersHandler(usersDao)));
        httpServer.createContext("/", HttpUtils.safe(linksHandler::redirect));
        executor = Executors.newVirtualThreadPerTaskExecutor();
        httpServer.setExecutor(executor);
        httpServer.start();
        return httpServer;
    }

    private void closeStorage() {
        try (PersistentDao _ = links; PersistentDao _ = users) {
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close storage", e);
        } finally {
            links = null;
            users = null;
        }
    }

    private static void status(HttpExchange exchange, PersistentDao linksDao, PersistentDao usersDao)
        throws IOException {
        if (!"/v0/status".equals(exchange.getRequestURI().getPath())) {
            HttpUtils.sendEmpty(exchange, HttpURLConnection.HTTP_NOT_FOUND);
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpUtils.sendEmpty(exchange, HttpURLConnection.HTTP_BAD_METHOD);
            return;
        }
        boolean healthy = linksDao.isWritable() && usersDao.isWritable();
        HttpUtils.sendEmpty(exchange, healthy ? HttpURLConnection.HTTP_OK : HttpURLConnection.HTTP_UNAVAILABLE);
    }
}
