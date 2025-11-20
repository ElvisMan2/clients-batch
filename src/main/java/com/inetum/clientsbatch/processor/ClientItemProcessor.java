package com.inetum.clientsbatch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inetum.clientsbatch.model.Data;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ClientItemProcessor implements ItemProcessor<Data, Data> {

    private final RestTemplate restTemplate;
    private final String apiUrl = "http://localhost:8081/api-simulation-loans/api/clients";
    private final String simulationApiUrl = "http://localhost:8081/api-simulation-loans/simulations/client/";

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

                Long clientId = new ObjectMapper()
                        .readTree(response.getBody())
                        .get(1)
                        .get("clientId")
                        .asLong();

                data.setClientId(clientId);
                System.out.println("✓ Cliente creado: id: " + clientId + " nombre: " + data.getFirstName());

                // Segunda llamada: Crear simulación
                if (createSimulation(data, clientId, headers)) {
                    return data;
                } else {
                    return null;
                }

            } else {
                System.err.println("Error al enviar cliente: " + response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            System.err.println("⚠ Error consumiendo API para cliente " + data.getFirstName());
            e.printStackTrace();
            return null;
        }
    }

    private boolean createSimulation(Data data, Long clientId, HttpHeaders headers) {
        try {
            Map<String, Object> simulationPayload = new HashMap<>();
            simulationPayload.put("loanAmount", data.getLoanAmount());
            simulationPayload.put("currency", data.getCurrency());
            simulationPayload.put("interestRate", data.getInterestRate());
            simulationPayload.put("term", data.getTerm());

            // Formatear la fecha a dd/MM/yyyy
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            simulationPayload.put("disbursementDate", data.getDisbursementDate().format(formatter));

            HttpEntity<Map<String, Object>> simulationRequest = new HttpEntity<>(simulationPayload, headers);
            String simulationUrl = simulationApiUrl + clientId;

            var simulationResponse = restTemplate.postForEntity(simulationUrl, simulationRequest, String.class);

            if (simulationResponse.getStatusCode().is2xxSuccessful()) {
                System.out.println("✓ Simulación creada para cliente: " + clientId);
                return true;
            } else {
                System.err.println("✗ Error al crear simulación para cliente " + clientId + ": " + simulationResponse.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            System.err.println("⚠ Error creando simulación para cliente " + clientId);
            e.printStackTrace();
            return false;
        }
    }
}
