package com.inetum.clientsbatch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inetum.clientsbatch.dto.Data;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class ClientItemProcessor implements ItemProcessor<Data, Data> {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl = "http://localhost:8081/api-simulation-loans/api/clients";
    private final String simulationApiUrl = "http://localhost:8081/api-simulation-loans/simulations/client/";
    private final String loanApiUrl = "http://localhost:8082/api-generation-loans/loans/generate/simulation/";

    public ClientItemProcessor() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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
                var jsonNode = objectMapper.readTree(response.getBody()).get(1);

                Long clientId = jsonNode.get("clientId").asLong();
                data.setClientId(clientId);

                if(response.getStatusCode().value()== 201)
                System.out.println("✓ Cliente creado: id: " + clientId + " nombre: " + data.getFirstName());

                if(response.getStatusCode().value()== 200)
                    System.out.println("✓ Cliente existe: id: " + clientId + " nombre: " + data.getFirstName());

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
                var jsonNode = objectMapper.readTree(simulationResponse.getBody()).get(1);

                Long simulationId = jsonNode.get("simulationId").asLong();
                data.setSimulationId(simulationId);

                Boolean approved = jsonNode.get("approved").asBoolean();
                data.setApproved(approved);

                Double monthlyPayment = jsonNode.get("monthlyPayment").asDouble();
                data.setMonthlyPayment(monthlyPayment);

                Double totalPayment = jsonNode.get("totalPayment").asDouble();
                data.setTotalPayment(totalPayment);

                System.out.println("✓ Simulación creada para cliente: " + clientId +
                        " | simulationId: " + simulationId +
                        " | approved: " + approved);

                // Tercera llamada: Crear préstamo solo si está aprobado
                if (approved) {
                    return createLoan(data, simulationId, headers);
                } else {
                    System.out.println("⚠ Simulación no aprobada para cliente: " + clientId + " - No se creará el préstamo");
                    return true;
                }

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

    private boolean createLoan(Data data, Long simulationId, HttpHeaders headers) {
        try {
            HttpEntity<Void> loanRequest = new HttpEntity<>(headers);
            String loanUrl = loanApiUrl + simulationId;

            var loanResponse = restTemplate.postForEntity(loanUrl, loanRequest, String.class);

            if (loanResponse.getStatusCode().is2xxSuccessful()) {
                var jsonNode = objectMapper.readTree(loanResponse.getBody());
                Long loanId = jsonNode.get("loanId").asLong();
                data.setLoanId(loanId);

                String dueDateString = jsonNode.path("payment").get(0).path("dueDate").asText();
                LocalDate dueDate = LocalDate.parse(dueDateString);
                data.setNextPaymentDate(dueDate);

                data.setTotalInterest(data.getMonthlyPayment()*data.getTerm()-data.getLoanAmount());
                System.out.println("✓ Préstamo creado: loanId: " + loanId + " para simulación: " + simulationId);
                return true;
            } else {
                System.err.println("✗ Error al crear préstamo para simulación " + simulationId + ": " + loanResponse.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            System.err.println("⚠ Error creando simulación para cliente " + simulationId);
            e.printStackTrace();
            return false;
        }
    }
}
