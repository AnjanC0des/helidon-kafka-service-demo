package com.example.myproject;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.common.serialization.StringDeserializer;

import io.helidon.config.Config;
import io.helidon.messaging.Channel;
import io.helidon.messaging.Emitter;
import io.helidon.messaging.Messaging;
import io.helidon.messaging.connectors.kafka.KafkaConnector;
import io.helidon.websocket.WsListener;
import io.helidon.websocket.WsSession;
//import jakarta.json.JsonObject;
//import jakarta.json.bind.Jsonb;
//import jakarta.json.bind.JsonbBuilder;

public class MyService implements WsListener {
	private final Config config = Config.create();
//    private final Messaging messaging;

	private final HashMap<WsSession,Messaging> messagingRegister=new HashMap<>();
	private final HashMap<WsSession,Emitter<String>> emitterRegister=new HashMap<>();
	 
	@Override
	public void onOpen(WsSession session){
		String kafkaServer = config.get("app.kafka.bootstrap.servers").asString().get();
	    String topic = config.get("app.kafka.topic").asString().get();
	    KafkaConnector kafkaConnector = KafkaConnector.create();
	    Channel<String> toProcessor = Channel.create();

	    Emitter<String> emitter = Emitter.create(toProcessor);
		Channel<String> toKafka = Channel.<String>builder()
	            .subscriberConfig(KafkaConnector.configBuilder()
	                    .bootstrapServers(kafkaServer)
	                    .topic(topic)
	                    .keyDeserializer(StringDeserializer.class)
	                    .valueDeserializer(StringDeserializer.class)
	                    .build()
	            )
	            .build();
		Messaging messaging = Messaging.builder()
	            .emitter(emitter)
	            .processor(toProcessor, toKafka, String::toUpperCase)
	            .connector(kafkaConnector)
	            .build()
	            .start();
		messagingRegister.put(session, messaging);
		emitterRegister.put(session,emitter);
	}

	@Override
    public void onMessage(WsSession session,String message,boolean last) {
		Emitter<String> emitter=emitterRegister.get(session);
		emitter.send(message);
		session.send("Recieved -> "+message, last);
   }

   @Override 
   public void onClose(WsSession session,int status, String reason){
//	  	sessions.remove(id.get()); remove from session store
	   emitterRegister.remove(session);
	   Optional.ofNullable(messagingRegister.remove(session))
       .ifPresent(Messaging::stop);
	}	


//   public Message parseMessage(String message){
//		Jsonb jsonb= JsonbBuilder.create();
//		return jsonb.fromJson(message,Message.class);
//	}
}