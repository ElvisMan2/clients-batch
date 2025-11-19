package com.inetum.clientsbatch.writer;

import com.inetum.clientsbatch.model.Client;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReportWriter implements ItemWriter<Client> {

    private final RestTemplate restTemplate;
    private final String apiUrl = "http://localhost:8081/api-simulation-loans/api/clients";
    private int totalWritten = 0;

    public ReportWriter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void write(Chunk<? extends Client> chunk) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (Client client : chunk.getItems()) {

            Map<String, Object> payload = new HashMap<>();
            payload.put("firstName", client.getFirstName());
            payload.put("paternalLastName", client.getPaternalLastName());
            payload.put("maternalLastName", client.getMaternalLastName());
            payload.put("currencyOfIncome", client.getCurrencyOfIncome());
            payload.put("monthlyIncome", client.getMonthlyIncome());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            try {
                var response = restTemplate.postForEntity(apiUrl, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    System.out.println("✔ Cliente enviado: " + client.getFirstName());
                    totalWritten++;
                } else {
                    System.err.println("✘ Error al enviar cliente: " + response.getStatusCode());
                }

            } catch (Exception e) {
                System.err.println("⚠ Error consumiendo API para cliente " + client.getFirstName());
                e.printStackTrace();  // O guardar en un log personalizado
            }
        }
    }


    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        int readCount = (int) stepExecution.getReadCount();

        try (FileWriter writer = new FileWriter("report.txt")) {
            writer.write("Step Summary:\n");
            writer.write("Total Records Read: " + readCount + "\n");
            writer.write("Total Records Written: " + totalWritten + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
