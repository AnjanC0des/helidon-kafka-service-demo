# helidon-kafka-nats-demo

* In this demo, a Helidon SE websocket microservice reads messages through websockets and publishes them to Kafka. 
* The messages are consumed and re-routed to the recipient using NATS.
* This setup decouples the producers/consumers and each component can scale independently. Message delivery has high throughput and durability
with low latency and event driven architecture. 
* We can also address high number of 'groups' that would be neither convenient nor managable using Kafka topics/partitons alone.
