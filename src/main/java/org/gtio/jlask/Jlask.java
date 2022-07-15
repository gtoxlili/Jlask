package org.gtio.jlask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

interface Handler {
    Response handle(Request request) throws InvocationTargetException, IllegalAccessException;
}

public class Jlask {
    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final Map<Integer, Handler> handlers = new HashMap<>();
    private final Map<Integer, Handler> errorHandlers = new HashMap<>();
    private int PoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;

    public Jlask(String ip, int port, int poolSize, Object instanceObject) throws IOException {
        serverSocket = new ServerSocket(port, poolSize * 4, InetAddress.getByName(ip));
        PoolSize = poolSize;
        executor = Executors.newFixedThreadPool(PoolSize);
        registerAllHandlers(instanceObject);
        startPrint(instanceObject.getClass().getName());
    }

    public Jlask(String ip, int port, Object instanceObject) throws IOException {
        serverSocket = new ServerSocket(port, PoolSize * 4, InetAddress.getByName(ip));
        executor = Executors.newFixedThreadPool(PoolSize);
        registerAllHandlers(instanceObject);
        startPrint(instanceObject.getClass().getName());
    }

    private void startPrint(String instanceName) {
        System.out.println(" * Jlask Server Started");
        System.out.println(" * Started at " + new Date());
        System.out.println(" * PoolSize: " + PoolSize);
        System.out.println(" * Serving Jlask app to '" + instanceName + "' (lazy loading)");
        System.out.println(" * Running on http://" + serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort());
        System.out.println(" * Press Ctrl+C to stop\n");
    }

    private void registerAllHandlers(Object instanceObject) {
        for (Method clazz : instanceObject.getClass().getMethods()) {
            Route route = clazz.getAnnotation(Route.class);
            ErrorHandler errorHandler = clazz.getAnnotation(ErrorHandler.class);
            if (route != null) {
                for (String reqType : route.method()) {
                    registerHandler(route.url(), reqType, clazz, instanceObject);
                }
            } else if (errorHandler != null) {
                registerErrorHandler(errorHandler.value(), clazz, instanceObject);
            }
        }
    }

    private void registerHandler(String target, String reqType, Method method, Object instanceObject) {
        handlers.put((target + reqType).hashCode(), req -> (Response) method.invoke(instanceObject, req));
    }

    private void registerErrorHandler(int status, Method method, Object instanceObject) {
        errorHandlers.put(status, req -> (Response) method.invoke(instanceObject, req));
    }

    public void start() {
        new Thread(() -> {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    executor.execute(() -> {
                        try {
                            InputStream in = socket.getInputStream();
                            OutputStream out = socket.getOutputStream();

                            try {
                                Request req = new Request(in);
                                socket.shutdownInput();

                                if (req.error) {
                                    System.out.println(" * Error: 网络异常 | 傻逼Chrome预加载的第二次加载 导致的请求丢失");
                                    socket.close();
                                    return;
                                }

                                Response res;
                                Integer urlMethodHash = (req.getUrl() + req.getMethod()).hashCode();
                                // 如果请求的路径不存在，则返回404错误
                                if (handlers.containsKey(urlMethodHash)) {
                                    res = handlers.get(urlMethodHash).handle(req);
                                } else {
                                    if (errorHandlers.containsKey(404)) {
                                        res = errorHandlers.get(404).handle(req);
                                    } else {
                                        res = Response.ErrorStatus(ErrorType.Err_404);
                                    }
                                }

                                // 如果存在错误处理器，则返回错误处理器的结果
                                if (res.getStatus() != 200) {
                                    if (errorHandlers.containsKey(res.getStatus())) {
                                        res = errorHandlers.get(res.getStatus()).handle(req);
                                    }
                                }

                                writerResponse(res, out);
                                socket.shutdownOutput();

                                String time = String.format("%.2f", (System.nanoTime() - req._firstGetByteTime) / 1000000f);
                                System.out.println(" " + req.getMethod() + " " + req.getUrlParams() + " " + res.getVersion() + " " + res.getStatus() + " " + time + " ms");

                            } catch (Exception e) {
                                writerResponse(Response.ErrorStatus(ErrorType.Err_500), out);
                            } finally {
                                socket.close();
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void writerResponse(Response res, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out, true);
        writer.println(res.getVersion() + res.getStatus());

        for (Map.Entry<String, String> entry : res.getHeaders().entrySet()) {
            writer.println(entry.getKey() + ": " + entry.getValue());
        }

        writer.println();
        if (res.getBody() != null) {
            writer.println(res.getBody());
        } else {
            out.write(res.getBinaryBody());
        }
    }
}
