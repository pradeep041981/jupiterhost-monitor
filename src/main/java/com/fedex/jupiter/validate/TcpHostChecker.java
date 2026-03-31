package com.fedex.jupiter.validate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpHostChecker implements HostChecker {
    private final String host;
    private final int port;
    private final int timeoutMillis;

    public TcpHostChecker(String host, int port, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public boolean isHostUp() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}

