package com.inetum.clientsbatch.processor;

import com.inetum.clientsbatch.model.Data;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class ClientItemProcessor implements ItemProcessor<Data, Data> {

    private final RestTemplate restTemplate;
    private final String apiUrl = "http://localhost:8081/api-simulation-loans/api/clients";

    public ClientItemProcessor() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Data process(Data data) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", data.getFirstName());
        payload.put("paternalLastName", data.getPaternalLastName());
        payload.put("maternalLastName", data.getMaternalLastName());
        payload.put("currencyOfIncome", data.getCurrencyOfIncome());
        payload.put("monthlyIncome", data.getMonthlyIncome());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            var response = restTemplate.postForEntity(apiUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✔ Cliente enviado: " + data.getFirstName());
                return data;
            } else {
                System.err.println("✘ Error al enviar cliente: " + response.getStatusCode());
                return null; // No procesar este cliente
            }

        } catch (Exception e) {
            System.err.println("⚠ Error consumiendo API para cliente " + data.getFirstName());
            e.printStackTrace();
            return null; // No procesar este cliente
        }
    }
}
