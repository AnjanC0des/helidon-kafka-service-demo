package com.example.myproject;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.kafka.common.serialization.StringSerializer;

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

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Dispatcher;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Headers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MyService implements WsListener {
	private final Config config = Config.create();
//    private final Messaging messaging;

	private final HashMap<WsSession,Messaging> messagingRegister=new HashMap<>();
	private final HashMap<WsSession,Emitter<String>> emitterRegister=new HashMap<>();
	 
	private static ConcurrentHashMap<String,String>map;
	private AtomicReference id;
	MyService(ConcurrentHashMap<String,String> map){
		this.map=map;
		this.id=new AtomicReference();
	}
	@Override
	public Optional<Headers> onHttpUpgrade(HttpPrologue prologue, Headers headers){
		this.id.set(prologue.query().get("id"));
		System.out.println("id set to"+id);
		//if(map.containsKey(id));
		return Optional.empty();
	}

	@Override
	public void onOpen(WsSession session){
		String kafkaServer = config.get("app.kafka.bootstrap.servers").asString().get();
	    String topic = config.get("app.kafka.topic").asString().get();
	    KafkaConnector kafkaConnector = KafkaConnector.create();

		Channel<String> toKafka = Channel.<String>builder()
					.subscriberConfig(KafkaConnector.configBuilder()
							.bootstrapServers(kafkaServer)
	                    .topic(topic)
	                    .keySerializer(StringSerializer.class)
	                    .valueSerializer(StringSerializer.class)
	                    .build()
	            )
	            .build();

	    Emitter<String> emitter = Emitter.create(toKafka);
		Messaging messaging = Messaging.builder()
	            .emitter(emitter)
	            .connector(kafkaConnector)
	            .build()
	            .start();
		messagingRegister.put(session, messaging);
		emitterRegister.put(session,emitter);

		String natsURL = "nats://127.0.0.1:4222";
        Connection nc = null;
		try{
			nc = Nats.connect(natsURL);
            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
					String m=new String(msg.getData(), StandardCharsets.UTF_8);
					System.out.println(m);
                    session.send(m,true);
            });
            dispatcher.subscribe("messages."+this.id.get());
			System.out.println("Subscribed to "+this.id.get());
		}
		catch(Exception e){
			System.out.println("Something seems to be wrong with NATS.");
		}
	}

	@Override
    public void onMessage(WsSession session,String message,boolean last) {
		Emitter<String> emitter=emitterRegister.get(session);
		emitter.send(message);
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
