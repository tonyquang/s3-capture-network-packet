package com.facebook.capturepacket.services;

import com.facebook.capturepacket.configuration.KafkaConfig;
import com.facebook.capturepacket.model.NetworkPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Handler extends Thread {
    public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) (.*)",
            Pattern.CASE_INSENSITIVE);
    private final Socket clientSocket;
    private boolean previousWasR = false;
    private final String userName = System.getProperty("user.name");

    KafkaTemplate<String, NetworkPacket> kafkaJsonTemplate;
    private final String addressKafkaServer;
    private final String topicKafka;

    public Handler(Socket clientSocket, String addressKafkaServer, String topicKafka) {
        this.clientSocket = clientSocket;
        this.addressKafkaServer = addressKafkaServer;
        this.topicKafka = topicKafka;
    }

    @Override
    public void run() {
        OutputStreamWriter outputStreamWriter = null;
        try {
            kafkaJsonTemplate = new KafkaConfig().createKafkaTemplate(this.addressKafkaServer);
            String request = readLine(clientSocket);
            kafkaJsonTemplate.send(this.topicKafka, createMessage(request));
            Matcher matcher = CONNECT_PATTERN.matcher(request);
            if (matcher.matches()) {
                String header;
                do {
                    header = readLine(clientSocket);
                } while (!"".equals(header));
                outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                        StandardCharsets.ISO_8859_1);

                final Socket forwardSocket;
                try {
                    forwardSocket = new Socket(matcher.group(1), Integer.parseInt(matcher.group(2)));
                } catch (IOException | NumberFormatException e) {
                    log.error("Forward socket error", e);
                    outputStreamWriter.write("HTTP/" + matcher.group(3) + " 502 Bad Gateway\r\n");
                    outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                    outputStreamWriter.write("\r\n");
                    outputStreamWriter.flush();
                    return;
                }
                try {
                    outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
                    outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                    outputStreamWriter.write("\r\n");
                    outputStreamWriter.flush();

                    Thread remoteToClient = new Thread() {
                        @Override
                        public void run() {
                            forwardData(forwardSocket, clientSocket);
                        }
                    };
                    remoteToClient.start();
                    try {
                        if (previousWasR) {
                            int read = clientSocket.getInputStream().read();
                            if (read != -1) {
                                if (read != '\n') {
                                    forwardSocket.getOutputStream().write(read);
                                }
                                forwardData(clientSocket, forwardSocket);
                            } else {
                                if (!forwardSocket.isOutputShutdown()) {
                                    forwardSocket.shutdownOutput();
                                }
                                if (!clientSocket.isInputShutdown()) {
                                    clientSocket.shutdownInput();
                                }
                            }
                        } else {
                            forwardData(clientSocket, forwardSocket);
                        }
                    } finally {
                        try {
                            remoteToClient.join();
                        } catch (InterruptedException e) {
                            log.error("Remote to client error", e);
                            Thread.currentThread().interrupt();
                        }
                    }
                } finally {
                    forwardSocket.close();
                }
            }
        } catch (IOException e) {
            log.error("Handler error", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Close socket error", e);
            }
        }
    }

    private static void forwardData(Socket inputSocket, Socket outputSocket) {
        try {
            InputStream inputStream = inputSocket.getInputStream();
            try {
                OutputStream outputStream = outputSocket.getOutputStream();
                try {
                    byte[] buffer = new byte[4096];
                    int read;
                    do {
                        read = inputStream.read(buffer);
                        if (read > 0) {
                            outputStream.write(buffer, 0, read);
                            if (inputStream.available() < 1) {
                                outputStream.flush();
                            }
                        }
                    } while (read >= 0);
                } finally {
                    if (!outputSocket.isOutputShutdown()) {
                        outputSocket.shutdownOutput();
                    }
                }
            } finally {
                if (!inputSocket.isInputShutdown()) {
                    inputSocket.shutdownInput();
                }
            }
        } catch (IOException e) {
            log.error("Forward Data error", e);
        }
    }

    private String readLine(Socket socket) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int next;
        readerLoop:
        while ((next = socket.getInputStream().read()) != -1) {
            if (previousWasR && next == '\n') {
                previousWasR = false;
                continue;
            }
            previousWasR = false;
            switch (next) {
                case '\r':
                    previousWasR = true;
                    break readerLoop;
                case '\n':
                    break readerLoop;
                default:
                    byteArrayOutputStream.write(next);
                    break;
            }
        }
        return byteArrayOutputStream.toString(StandardCharsets.ISO_8859_1);
    }

    private NetworkPacket createMessage(String msg) {
        return new NetworkPacket().builder()
                .message(msg)
                .userName(userName)
                .timeStamp(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()))
                .build();
    }
}
