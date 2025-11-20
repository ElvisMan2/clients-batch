package com.inetum.clientsbatch.writer;

import com.inetum.clientsbatch.dto.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportWriterTest {

    private ReportWriter reportWriter;

    @Mock
    private StepExecution stepExecution;

    private static final String REPORT_FILE = "report.txt";

    @BeforeEach
    void setUp() {
        reportWriter = new ReportWriter();
        deleteReportFile();
    }

    @AfterEach
    void tearDown() {
        deleteReportFile();
    }

    private void deleteReportFile() {
        File file = new File(REPORT_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testWriteWithApprovedLoans() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan));

        // Act
        reportWriter.write(chunk);

        // Assert - No debe lanzar excepciones
        assertDoesNotThrow(() -> reportWriter.write(chunk));
    }

    @Test
    void testWriteWithNonApprovedLoans() throws Exception {
        // Arrange - Préstamo sin loanId (no aprobado)
        Data nonApprovedLoan = Data.builder()
                .clientId(2L)
                .firstName("María")
                .paternalLastName("Fernández")
                .maternalLastName("González")
                .simulationId(200L)
                .approved(false)
                .loanId(null) // Sin préstamo generado
                .build();

        Chunk<Data> chunk = new Chunk<>(List.of(nonApprovedLoan));

        // Act
        reportWriter.write(chunk);

        // Assert - No debe lanzar excepciones
        assertDoesNotThrow(() -> reportWriter.write(chunk));
    }

    @Test
    void testWriteMixedLoans() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Data nonApprovedLoan = Data.builder()
                .clientId(2L)
                .firstName("María")
                .loanId(null)
                .build();

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan, nonApprovedLoan));

        // Act
        reportWriter.write(chunk);

        // Assert
        assertDoesNotThrow(() -> reportWriter.write(chunk));
    }

    @Test
    void testAfterStepCreatesReportFile() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        File reportFile = new File(REPORT_FILE);
        assertTrue(reportFile.exists(), "El archivo de reporte debe ser creado");
    }

    @Test
    void testReportContainsCorrectHeaders() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("REPORTE DE PRÉSTAMOS GENERADOS"));
        assertTrue(content.contains("ID Cliente"));
        assertTrue(content.contains("Nombre"));
        assertTrue(content.contains("ID Préstamo"));
        assertTrue(content.contains("Moneda"));
    }

    @Test
    void testReportContainsCorrectStatistics() throws Exception {
        // Arrange
        Data approvedLoan1 = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Data approvedLoan2 = createTestData(2L, "María", "Fernández", "González",
                200L, "EUR", 20000.0, 600.0,
                LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 22),
                36, 620.50);

        Data nonApprovedLoan = Data.builder()
                .clientId(3L)
                .firstName("Carlos")
                .loanId(null)
                .build();

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan1, approvedLoan2, nonApprovedLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Total de registros procesados: 3"));
        assertTrue(content.contains("Total de préstamos generados: 2"));
        assertTrue(content.contains("Simulaciones no aprobadas: 1"));
    }

    @Test
    void testReportContainsLoanDetails() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Juan"));
        assertTrue(content.contains("García"));
        assertTrue(content.contains("López"));
        assertTrue(content.contains("100")); // loanId
        assertTrue(content.contains("USD"));
        assertTrue(content.contains("15000.00"));
        assertTrue(content.contains("500.00"));
        assertTrue(content.contains("20/12/2025"));
        assertTrue(content.contains("20/01/2026"));
        assertTrue(content.contains("24"));
        assertTrue(content.contains("681.84"));
    }

    @Test
    void testReportWithMultipleLoans() throws Exception {
        // Arrange
        Data loan1 = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Data loan2 = createTestData(2L, "María", "Fernández", "González",
                200L, "EUR", 20000.0, 600.0,
                LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 22),
                36, 620.50);

        Data loan3 = createTestData(3L, "Carlos", "Rodríguez", "Martínez",
                300L, "MXN", 50000.0, 1200.0,
                LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 25),
                48, 1250.75);

        Chunk<Data> chunk = new Chunk<>(List.of(loan1, loan2, loan3));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Juan"));
        assertTrue(content.contains("María"));
        assertTrue(content.contains("Carlos"));
        assertTrue(content.contains("Total de préstamos generados: 3"));
    }

    @Test
    void testReportWithNoLoans() throws Exception {
        // Arrange - Chunk vacío
        Chunk<Data> chunk = new Chunk<>();
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Total de registros procesados: 0"));
        assertTrue(content.contains("Total de préstamos generados: 0"));
        assertTrue(content.contains("Simulaciones no aprobadas: 0"));
    }

    @Test
    void testReportWithOnlyNonApprovedLoans() throws Exception {
        // Arrange
        Data nonApproved1 = Data.builder().clientId(1L).firstName("Juan").loanId(null).build();
        Data nonApproved2 = Data.builder().clientId(2L).firstName("María").loanId(null).build();

        Chunk<Data> chunk = new Chunk<>(List.of(nonApproved1, nonApproved2));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Total de registros procesados: 2"));
        assertTrue(content.contains("Total de préstamos generados: 0"));
        assertTrue(content.contains("Simulaciones no aprobadas: 2"));
        assertFalse(content.contains("Juan")); // No debe aparecer en el detalle
        assertFalse(content.contains("María"));
    }

    @Test
    void testDateFormattingInReport() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 5),
                24, 681.84);

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("20/12/2025"), "Debe contener la fecha de desembolso formateada");
        assertTrue(content.contains("05/01/2026"), "Debe contener la fecha del próximo pago formateada");
    }

    @Test
    void testReportWithDifferentCurrencies() throws Exception {
        // Arrange
        Data usdLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Data eurLoan = createTestData(2L, "María", "Fernández", "González",
                200L, "EUR", 20000.0, 600.0,
                LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 22),
                36, 620.50);

        Data mxnLoan = createTestData(3L, "Carlos", "Rodríguez", "Martínez",
                300L, "MXN", 50000.0, 1200.0,
                LocalDate.of(2025, 12, 25), LocalDate.of(2026, 1, 25),
                48, 1250.75);

        Chunk<Data> chunk = new Chunk<>(List.of(usdLoan, eurLoan, mxnLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("USD"));
        assertTrue(content.contains("EUR"));
        assertTrue(content.contains("MXN"));
    }

    @Test
    void testReportWithLargeAmounts() throws Exception {
        // Arrange
        Data largeLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 500000.0, 50000.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                60, 9500.50);

        Chunk<Data> chunk = new Chunk<>(List.of(largeLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("500000.00"));
        assertTrue(content.contains("50000.00"));
        assertTrue(content.contains("9500.50"));
    }

    @Test
    void testMultipleWriteCalls() throws Exception {
        // Arrange
        Data loan1 = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Data loan2 = createTestData(2L, "María", "Fernández", "González",
                200L, "EUR", 20000.0, 600.0,
                LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 22),
                36, 620.50);

        Chunk<Data> chunk1 = new Chunk<>(List.of(loan1));
        Chunk<Data> chunk2 = new Chunk<>(List.of(loan2));

        // Act
        reportWriter.write(chunk1);
        reportWriter.write(chunk2);
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("Total de registros procesados: 2"));
        assertTrue(content.contains("Total de préstamos generados: 2"));
        assertTrue(content.contains("Juan"));
        assertTrue(content.contains("María"));
    }

    @Test
    void testReportContainsSeparators() throws Exception {
        // Arrange
        Data approvedLoan = createTestData(1L, "Juan", "García", "López",
                100L, "USD", 15000.0, 500.0,
                LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 20),
                24, 681.84);

        Chunk<Data> chunk = new Chunk<>(List.of(approvedLoan));
        reportWriter.write(chunk);

        // Act
        reportWriter.afterStep(stepExecution);

        // Assert
        String content = Files.readString(Paths.get(REPORT_FILE), StandardCharsets.UTF_8);
        assertTrue(content.contains("=".repeat(120)), "Debe contener separadores");
    }

    private Data createTestData(Long clientId, String firstName, String paternalLastName,
                                String maternalLastName, Long loanId, String currency,
                                Double loanAmount, Double totalInterest,
                                LocalDate disbursementDate, LocalDate nextPaymentDate,
                                Integer term, Double monthlyPayment) {
        return Data.builder()
                .clientId(clientId)
                .firstName(firstName)
                .paternalLastName(paternalLastName)
                .maternalLastName(maternalLastName)
                .loanId(loanId)
                .currency(currency)
                .loanAmount(loanAmount)
                .totalInterest(totalInterest)
                .disbursementDate(disbursementDate)
                .nextPaymentDate(nextPaymentDate)
                .term(term)
                .monthlyPayment(monthlyPayment)
                .build();
    }
}
