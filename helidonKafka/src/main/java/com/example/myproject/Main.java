
package com.example.myproject;


import io.helidon.logging.common.LogConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.websocket.WsRouting;
import io.helidon.websocket.WsSession;


/**
 * The application main class.
 */
public class Main {


    /**
     * Cannot be instantiated.
     */
    private Main() {
    }


    
    
//    private static final Logger log= Logger.getLogger(Main.class.getName());
//	private static final ConcurrentHashMap<String,WsSession> sessions= new ConcurrentHashMap<>();
    public static void main(String[] args) {
        
        // load logging configuration
        LogConfig.configureRuntime();

        Config config = Config.create();
        Config.global(config);


        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .routing(Main::routing)
                .addRouting(	
                		WsRouting.builder()
                            .endpoint("/message",()-> new MyService()))
		.build()
                .start();


        System.out.println("WEB server is up! http://localhost:" + server.port() + "/simple-greet");
	ConsumerService.run();

    }


    /**
     * Updates HTTP Routing.
     */
    static void routing(HttpRouting.Builder routing) {
        routing
               .register("/greet", new GreetService())
               .get("/simple-greet", (req, res) -> res.send("Hello World!")); 
    }
}
