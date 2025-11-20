package com.inetum.clientsbatch.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inetum.clientsbatch.dto.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientItemProcessorTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ClientItemProcessor processor;

    private Data testData;
    private static final String CLIENT_API_URL = "http://localhost:8081/api-simulation-loans/api/clients";
    private static final String SIMULATION_API_URL = "http://localhost:8081/api-simulation-loans/simulations/client/";
    private static final String LOAN_API_URL = "http://localhost:8082/api-generation-loans/loans/generate/simulation/";

    @BeforeEach
    void setUp() {
        processor = new ClientItemProcessor();
        ReflectionTestUtils.setField(processor, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(processor, "objectMapper", objectMapper);

        testData = Data.builder()
                .firstName("Juan")
                .paternalLastName("García")
                .maternalLastName("López")
                .currencyOfIncome("USD")
                .monthlyIncome(3000.0)
                .loanAmount(15000.0)
                .currency("USD")
                .interestRate(8.5)
                .term(24)
                .disbursementDate(LocalDate.of(2025, 12, 20))
                .build();
    }

    @Test
    void testProcessClientCreatedSuccessfully() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(1L, 201);
        String simulationResponse = createSimulationResponse(100L, true);
        String loanResponse = createLoanResponse(200L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getClientId());
        assertEquals(100L, result.getSimulationId());
        assertEquals(200L, result.getLoanId());
        assertTrue(result.getApproved());
        assertNotNull(result.getNextPaymentDate());
        verify(restTemplate, times(1)).postForEntity(eq(CLIENT_API_URL), any(), eq(String.class));
        verify(restTemplate, times(1)).postForEntity(contains(SIMULATION_API_URL), any(), eq(String.class));
        verify(restTemplate, times(1)).postForEntity(contains(LOAN_API_URL), any(), eq(String.class));
    }

    @Test
    void testProcessClientAlreadyExists() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(2L, 200);
        String simulationResponse = createSimulationResponse(101L, true);
        String loanResponse = createLoanResponse(201L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getClientId());
        assertEquals(101L, result.getSimulationId());
        assertEquals(201L, result.getLoanId());
    }

    @Test
    void testProcessSimulationNotApproved() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(3L, 201);
        String simulationResponse = createSimulationResponse(102L, false);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getClientId());
        assertEquals(102L, result.getSimulationId());
        assertFalse(result.getApproved());
        assertNull(result.getLoanId()); // No se crea préstamo si no está aprobado
        verify(restTemplate, times(1)).postForEntity(eq(CLIENT_API_URL), any(), eq(String.class));
        verify(restTemplate, times(1)).postForEntity(contains(SIMULATION_API_URL), any(), eq(String.class));
        verify(restTemplate, never()).postForEntity(contains(LOAN_API_URL), any(), eq(String.class));
    }

    @Test
    void testProcessClientCreationFails() throws Exception {
        // Arrange
        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).postForEntity(eq(CLIENT_API_URL), any(), eq(String.class));
        verify(restTemplate, never()).postForEntity(contains(SIMULATION_API_URL), any(), eq(String.class));
    }

    @Test
    void testProcessSimulationCreationFails() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(4L, 201);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNull(result);
        assertEquals(4L, testData.getClientId());
        verify(restTemplate, times(1)).postForEntity(contains(SIMULATION_API_URL), any(), eq(String.class));
        verify(restTemplate, never()).postForEntity(contains(LOAN_API_URL), any(), eq(String.class));
    }

    @Test
    void testProcessLoanCreationFails() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(5L, 201);
        String simulationResponse = createSimulationResponse(103L, true);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).postForEntity(contains(LOAN_API_URL), any(), eq(String.class));
    }

    @Test
    void testProcessClientApiThrowsException() throws Exception {
        // Arrange
        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).postForEntity(eq(CLIENT_API_URL), any(), eq(String.class));
    }

    @Test
    void testProcessSimulationApiThrowsException() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(6L, 201);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNull(result);
    }

    @Test
    void testProcessLoanApiThrowsException() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(7L, 201);
        String simulationResponse = createSimulationResponse(104L, true);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNull(result);
    }

    @Test
    void testProcessWithDifferentCurrencies() throws Exception {
        // Arrange
        testData.setCurrency("EUR");
        testData.setCurrencyOfIncome("EUR");

        String clientResponse = createClientResponse(8L, 201);
        String simulationResponse = createSimulationResponse(105L, true);
        String loanResponse = createLoanResponse(202L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals("EUR", result.getCurrency());
        assertEquals("EUR", result.getCurrencyOfIncome());
    }

    @Test
    void testProcessWithDifferentTerms() throws Exception {
        // Arrange
        testData.setTerm(48);

        String clientResponse = createClientResponse(9L, 201);
        String simulationResponse = createSimulationResponse(106L, true);
        String loanResponse = createLoanResponse(203L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals(48, result.getTerm());
    }

    @Test
    void testProcessCalculatesTotalInterest() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(10L, 201);
        String simulationResponse = createSimulationResponse(107L, true);
        String loanResponse = createLoanResponse(204L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTotalInterest());
        // totalInterest = monthlyPayment * term - loanAmount
        double expectedInterest = result.getMonthlyPayment() * result.getTerm() - result.getLoanAmount();
        assertEquals(expectedInterest, result.getTotalInterest(), 0.01);
    }

    @Test
    void testProcessSetsNextPaymentDate() throws Exception {
        // Arrange
        String clientResponse = createClientResponse(11L, 201);
        String simulationResponse = createSimulationResponse(108L, true);
        String loanResponse = createLoanResponse(205L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getNextPaymentDate());
        assertEquals(LocalDate.of(2026, 1, 20), result.getNextPaymentDate());
    }

    @Test
    void testProcessWithLargeAmounts() throws Exception {
        // Arrange
        testData.setLoanAmount(500000.0);
        testData.setMonthlyIncome(50000.0);

        String clientResponse = createClientResponse(12L, 201);
        String simulationResponse = createSimulationResponse(109L, true);
        String loanResponse = createLoanResponse(206L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals(500000.0, result.getLoanAmount());
    }

    @Test
    void testProcessFormatsDateCorrectly() throws Exception {
        // Arrange
        testData.setDisbursementDate(LocalDate.of(2026, 1, 15));

        String clientResponse = createClientResponse(13L, 201);
        String simulationResponse = createSimulationResponse(110L, true);
        String loanResponse = createLoanResponse(207L);

        when(restTemplate.postForEntity(eq(CLIENT_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(clientResponse, HttpStatus.CREATED));
        when(restTemplate.postForEntity(contains(SIMULATION_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(simulationResponse, HttpStatus.OK));
        when(restTemplate.postForEntity(contains(LOAN_API_URL), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(loanResponse, HttpStatus.OK));

        // Act
        Data result = processor.process(testData);

        // Assert
        assertNotNull(result);
        assertEquals(LocalDate.of(2026, 1, 15), result.getDisbursementDate());
    }

    // Helper methods to create mock responses

    private String createClientResponse(Long clientId, int statusCode) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        String message = statusCode == 201 ? "Client created successfully" : "User already exist";
        arrayNode.add(message);

        ObjectNode clientNode = mapper.createObjectNode();
        clientNode.put("clientId", clientId);
        clientNode.put("firstName", "Juan");
        clientNode.put("paternalLastName", "García");
        clientNode.put("maternalLastName", "López");
        clientNode.put("currencyOfIncome", "USD");
        clientNode.put("monthlyIncome", 3000.0);

        arrayNode.add(clientNode);

        return arrayNode.toString();
    }

    private String createSimulationResponse(Long simulationId, boolean approved) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();

        arrayNode.add("Loan simulation " + (approved ? "approved" : "rejected"));

        ObjectNode simNode = mapper.createObjectNode();
        simNode.put("simulationId", simulationId);
        simNode.put("loanAmount", 15000.0);
        simNode.put("currency", "USD");
        simNode.put("interestRate", 8.5);
        simNode.put("term", 24);
        simNode.put("monthlyPayment", 681.84);
        simNode.put("totalPayment", 16364.16);
        simNode.put("approved", approved);
        simNode.put("createdAt", "2025-11-20T07:00:00");
        simNode.put("disbursementDate", "20/12/2025");
        simNode.put("clientId", 1);

        arrayNode.add(simNode);

        return arrayNode.toString();
    }

    private String createLoanResponse(Long loanId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode loanNode = mapper.createObjectNode();

        loanNode.put("loanId", loanId);
        loanNode.put("loanAmount", 15000.0);
        loanNode.put("interestRate", 8.5);
        loanNode.put("term", 24);
        loanNode.put("installment", 681.84);
        loanNode.put("status", 1);
        loanNode.put("creationDate", "2025-11-20T07:00:00");
        loanNode.put("currency", "USD");
        loanNode.put("disbursementDate", "20/12/2025");
        loanNode.put("clientId", 1);

        ArrayNode paymentsArray = mapper.createArrayNode();
        ObjectNode payment = mapper.createObjectNode();
        payment.put("installmentId", 1);
        payment.put("paymentNumber", 1);
        payment.put("currency", "USD");
        payment.put("installment", 681.84);
        payment.put("amortization", 575.59);
        payment.put("interest", 106.25);
        payment.put("dueDate", "2026-01-20");
        payment.put("capitalBalance", 14424.41);

        paymentsArray.add(payment);
        loanNode.set("payment", paymentsArray);

        return loanNode.toString();
    }
}
