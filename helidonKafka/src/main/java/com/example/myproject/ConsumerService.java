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
import java.util.stream.Collectors;

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
    static void run() {
        parallelConsumer = setupParallelConsumer();

       // postSetup();

        // tag::example[]
        parallelConsumer.poll(record ->
                System.out.println("Concurrently processing a record: "+record)
        );
        // end::example[]
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
