package org.apache.juneau.server.config;

import org.apache.juneau.microservice.jetty.JettyMicroservice;
import org.apache.juneau.server.config.rest.LoadConfigResource;

public class App {

    public static void main(String[] args) throws Exception {
        JettyMicroservice
                .create()
                .args(args)
                .servlet(LoadConfigResource.class)
                .build()
                .start()
                .startConsole()
                .join();
    }
}
// ter o xml do jetty padrão na aplicação
// posibilitar a remoção do bin.xml