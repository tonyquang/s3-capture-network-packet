package com.facebook.capturepacket.model;

import org.apache.kafka.common.serialization.Serdes;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

public class NetworkPacketSerde extends Serdes.WrapperSerde<NetworkPacket> {
    public NetworkPacketSerde(){
        super(new JsonSerializer<>(), new JsonDeserializer<>(NetworkPacket.class));
    }
}
