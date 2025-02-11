package com.example.myproject;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.util.Arrays;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.lang.InterruptedException;
public class ConsumerService {
    private static volatile boolean running = true;
    public static void run()  {
	System.out.println("Consumer Started");
	String bootstrapserver="127.0.0.1:9092";
		//String topic="signal";
		String group="consumergroup";
		Properties props=new Properties();
		props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapserver);
		props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,group);
		props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList("messageTopic"));

	        // Register shutdown hook
        	Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            		running = false;
			System.out.println("Shutdown triggered");
            //		throw new InterruptedException("SHUTDOWN");
       		 }));

        	for (int i = 0; i < 10; i++) { // Start with 10 consumers
            		Thread.ofVirtual().start(() -> {

                		try {
                   			while (running) {
						ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                        			records.forEach(record -> {
                            			// Publish to Redis cache
                            			System.out.printf("Consumed record with key %s and value %s%n", record.key(), record.value());
                        			});
                    			}
                		} catch (WakeupException e) {
                    		// Ignore if shutting down
                    			if (running) throw e;
                		} finally {
                    			consumer.close();
                   			 System.out.println("Consumer closed.");
                		}
            		});
        	}
		
	}
}




