package com.facebook.capturepacket.services;


import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


@Slf4j
public class ProxyServer extends Thread {

    private final String addressKafkaServer;
    private final String topicKafka;

    public ProxyServer(String addressKafkaServer, String topicKafka) {
        super("Server Thread");
        this.addressKafkaServer = addressKafkaServer;
        this.topicKafka = topicKafka;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            Socket socket;
            log.info("Proxy server start on 127.0.0.1:9999, waiting for connection...");
            while ((socket = serverSocket.accept()) != null) {
                (new Handler(socket, this.addressKafkaServer, this.topicKafka)).start();
            }
        } catch (IOException e) {
           log.error("Start server error: ", e);
        }
    }

}
