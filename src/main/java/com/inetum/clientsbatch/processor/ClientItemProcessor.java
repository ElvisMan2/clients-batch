package com.inetum.clientsbatch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inetum.clientsbatch.dto.Data;
import com.inetum.clientsbatch.writer.ReportWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientItemProcessor implements ItemProcessor<Data, Data> {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(ClientItemProcessor.class);
    private static final String API_URL = "http://localhost:8081/api-simulation-loans/api/clients";
    private static final String SIMULATION_API_URL = "http://localhost:8081/api-simulation-loans/simulations/client/";
    private static final String LOAN_API_URL = "http://localhost:8082/api-generation-loans/loans/generate/simulation/";

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
            var response = restTemplate.postForEntity(API_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                var jsonNode = objectMapper.readTree(response.getBody()).get(1);

                Long clientId = jsonNode.get("clientId").asLong();
                data.setClientId(clientId);

                if(response.getStatusCode().value()== 201)
                    logger.info("✓ Cliente creado: id: " + clientId + " nombre: " + data.getFirstName());

                if(response.getStatusCode().value()== 200)
                    logger.info("✓ Cliente existe: id: " + clientId + " nombre: " + data.getFirstName());

                // Segunda llamada: Crear simulación
                if (createSimulation(data, clientId, headers)) {
                    return data;
                } else {
                    return null;
                }

            } else {
                logger.warn("Error al enviar cliente: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.warn("Error consumiendo API para cliente {}", data.getFirstName());
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
            String simulationUrl = SIMULATION_API_URL + clientId;

            var simulationResponse = restTemplate.postForEntity(simulationUrl, simulationRequest, String.class);

            if (simulationResponse.getStatusCode().is2xxSuccessful()) {
                var jsonNode = objectMapper.readTree(simulationResponse.getBody()).get(1);

                Long simulationId = jsonNode.get("simulationId").asLong();
                data.setSimulationId(simulationId);

                boolean approved = jsonNode.get("approved").asBoolean();
                data.setApproved(approved);

                Double monthlyPayment = jsonNode.get("monthlyPayment").asDouble();
                data.setMonthlyPayment(monthlyPayment);

                Double totalPayment = jsonNode.get("totalPayment").asDouble();
                data.setTotalPayment(totalPayment);

                logger.info(String.format("✓ Simulación creada para cliente: %s | simulationId: %s | approved: %s",
                        clientId, simulationId, approved));

                // Tercera llamada: Crear préstamo solo si está aprobado
                if (approved) {
                    return createLoan(data, simulationId, headers);
                } else {
                    logger.info("Simulación no aprobada para cliente: {} - No se creará el préstamo", clientId);
                    return true;
                }

            } else {
                logger.warn("Error al crear simulación para cliente {}: {}", clientId, simulationResponse.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.warn("Error creando simulación para cliente {}",clientId);
            return false;
        }
    }

    private boolean createLoan(Data data, Long simulationId, HttpHeaders headers) {
        try {
            HttpEntity<Void> loanRequest = new HttpEntity<>(headers);
            String loanUrl = LOAN_API_URL + simulationId;

            var loanResponse = restTemplate.postForEntity(loanUrl, loanRequest, String.class);

            if (loanResponse.getStatusCode().is2xxSuccessful()) {
                var jsonNode = objectMapper.readTree(loanResponse.getBody());
                Long loanId = jsonNode.get("loanId").asLong();
                data.setLoanId(loanId);

                String dueDateString = jsonNode.path("payment").get(0).path("dueDate").asText();
                LocalDate dueDate = LocalDate.parse(dueDateString);
                data.setNextPaymentDate(dueDate);

                data.setTotalInterest(data.getMonthlyPayment()*data.getTerm()-data.getLoanAmount());
                logger.info("Préstamo creado: loanId: {} para simulación: {}", loanId, simulationId);
                return true;
            } else {
                logger.warn("Error al crear préstamo para simulación {}: {}", simulationId, loanResponse.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            logger.warn("Error creando préstamo para simulación {}", simulationId);
            return false;
        }
    }
}
