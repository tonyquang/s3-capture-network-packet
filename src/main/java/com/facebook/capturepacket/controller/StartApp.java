package com.facebook.capturepacket.controller;

import com.facebook.capturepacket.services.ProxyServer;
import lombok.extern.slf4j.Slf4j;



@Slf4j
public class StartApp {

    ProxyServer startProxyServer;

    public void startApp(String kafkaAddress, String topicName){
        startProxyServer = new ProxyServer(kafkaAddress, topicName);
        startProxyServer.start();
    }

}
