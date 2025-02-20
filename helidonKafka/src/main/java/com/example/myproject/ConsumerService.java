package com.example.myproject;

import io.confluent.parallelconsumer.ParallelConsumerOptions;
import io.confluent.parallelconsumer.ParallelStreamProcessor;
import io.confluent.parallelconsumer.RecordContext;
//import lombok.Value;
//import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.concurrent.CircuitBreakingException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import static io.confluent.csid.utils.StringUtils.msg;
import static io.confluent.parallelconsumer.ParallelConsumerOptions.ProcessingOrder.KEY;
import static pl.tlinkowski.unij.api.UniLists.of;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Dispatcher;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
public class ConsumerService {
    static final String inputTopic = "messageTopic";
    //String outputTopic = "output-topic-" + RandomUtils.nextInt();

    static Consumer<String, String> getKafkaConsumer() {
	    String bootstrapserver="127.0.0.1:9092";
	    String group="consumergroup";
	    Properties props=new Properties();
	    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapserver);
		props.setProperty(ConsumerConfig.GROUP_ID_CONFIG,group);
		props.setProperty("enable.auto.commit", "false");
		props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
		props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    static Producer<String, String> getKafkaProducer() {
	    String bootstrapserver="127.0.0.1:9092";
	    Properties props=new Properties();
	    props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapserver);
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
	//	props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaProducer<>(props);
    }

    static ParallelStreamProcessor<String, String> parallelConsumer;

    @SuppressWarnings("UnqualifiedFieldAccess")
    static void run() throws InterruptedException{
        parallelConsumer = setupParallelConsumer();
        String natsURL = "nats://127.0.0.1:4222";
        Connection nc = null;
        try{
            nc = Nats.connect(natsURL);
            // Dispatcher dispatcher = nc.createDispatcher((msg) -> {
            //         System.out.printf("%s on subject %s\n",
            //                 new String(msg.getData(), StandardCharsets.UTF_8),
            //             msg.getSubject());
            // });
            // dispatcher.subscribe("messages.message");

            // while(true){
            //     nc.publish("messages.message","Message from consumer!!".getBytes(StandardCharsets.UTF_8));
            //     Thread.sleep(1000);
            // }
            final Connection fnc=nc;
            Thread consumerThread = new Thread(() -> {
                parallelConsumer.poll(record -> {
                    System.out.println("Concurrently processing a record: " + record);
                    byte[] messageBytes = record.value().getBytes(StandardCharsets.UTF_8);
                    try {
                        fnc.publish("messages.1", messageBytes);
                    } catch (Exception e) {
                        System.out.println("TRIED PUBLISHING TO NATS BUT FAILED");
                        e.printStackTrace();
                    }
                });
            });
    
            consumerThread.start();
            consumerThread.join();
            // final Connection fnc=nc;
            // parallelConsumer.poll(record ->{
            //     System.out.println("Concurrently processing a record: "+record);
            //     byte[] messageBytes= record.toString().getBytes(StandardCharsets.UTF_8);
            //     fnc.publish("messages.message",messageBytes);
            //     return;
            // });
        }catch(Exception e){
            System.out.println("CAUGHT FROM OUTER CATCH BLOCK.");
            e.printStackTrace();
        }
        finally{
            // if (nc != null) {
            //     try {
            //         nc.close();
            //     } catch (Exception e) {
            //         e.printStackTrace();
            //     }
            // }
        }
    }

    protected void postSetup() {
        // ignore
    }

    @SuppressWarnings({"FeatureEnvy", "MagicNumber"})
    static ParallelStreamProcessor<String, String> setupParallelConsumer() {
        // tag::exampleSetup[]
        Consumer<String, String> kafkaConsumer = getKafkaConsumer(); // <1>
        Producer<String, String> kafkaProducer = getKafkaProducer();

        var options = ParallelConsumerOptions.<String, String>builder()
                .ordering(KEY) // <2>
                .maxConcurrency(1000) // <3>
                .consumer(kafkaConsumer)
                .producer(kafkaProducer)
                .build();

        ParallelStreamProcessor<String, String> eosStreamProcessor =
                ParallelStreamProcessor.createEosStreamProcessor(options);

        eosStreamProcessor.subscribe(of(inputTopic)); // <4>

        return eosStreamProcessor;
        // end::exampleSetup[]
    }

     void close() {
        this.parallelConsumer.close();
    }
}
