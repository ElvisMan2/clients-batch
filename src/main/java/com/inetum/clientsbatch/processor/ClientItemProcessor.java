package com.inetum.clientsbatch.processor;

import com.inetum.clientsbatch.model.Client;
import org.springframework.batch.item.ItemProcessor;

public class ClientItemProcessor implements ItemProcessor<Client, Client> {
    @Override
    public Client process(Client client) throws Exception {
        // Example processing: Convert name and lastName to uppercase
        client.setFirstName(client.getFirstName());
        client.setPaternalLastName(client.getPaternalLastName());
        client.setMaternalLastName(client.getMaternalLastName());
        client.setCurrencyOfIncome(client.getCurrencyOfIncome());
        client.setMonthlyIncome(client.getMonthlyIncome());

        return client;
    }
}
