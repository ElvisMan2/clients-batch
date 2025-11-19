package com.inetum.clientsbatch.processor;

import com.inetum.clientsbatch.model.Client;
import org.springframework.batch.item.ItemProcessor;

public class ClientItemProcessor implements ItemProcessor<Client, Client> {
    @Override
    public Client process(Client client) throws Exception {
        // Example processing: Convert name and lastName to uppercase
        client.setName(client.getName().toUpperCase());
        client.setLastName(client.getLastName().toUpperCase());
        return client;
    }
}
