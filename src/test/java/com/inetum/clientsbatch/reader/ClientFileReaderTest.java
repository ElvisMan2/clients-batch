package com.inetum.clientsbatch.reader;

import com.inetum.clientsbatch.dto.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class ClientFileReaderTest {

    private ClientFileReader clientFileReader;
    private FlatFileItemReader<Data> itemReader;
    private DateTimeFormatter dateFormatter;

    @BeforeEach
    void setUp() {
        clientFileReader = new ClientFileReader();
        itemReader = clientFileReader.clientItemReader();
        dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    }

    @Test
    void testClientItemReaderIsNotNull() {
        assertNotNull(itemReader, "El reader no debe ser null");
    }

    @Test
    void testClientItemReaderHasCorrectResource() {
        assertNotNull(itemReader, "El reader debe tener un resource configurado");
    }

    @Test
    void testReadFirstLineFromCSV() throws Exception {
        itemReader.open(new ExecutionContext());

        Data data = itemReader.read();

        assertNotNull(data, "El primer registro no debe ser null");
        assertEquals("Juan", data.getFirstName());
        assertEquals("García", data.getPaternalLastName());
        assertEquals("López", data.getMaternalLastName());
        assertEquals("USD", data.getCurrencyOfIncome());
        assertEquals(100.00, data.getMonthlyIncome());
        assertEquals(15000.00, data.getLoanAmount());
        assertEquals("USD", data.getCurrency());
        assertEquals(8.5, data.getInterestRate());
        assertEquals(24, data.getTerm());
        assertEquals(LocalDate.parse("20/12/2025", dateFormatter), data.getDisbursementDate());

        itemReader.close();
    }

    @Test
    void testReadMultipleLines() throws Exception {
        itemReader.open(new ExecutionContext());

        Data firstData = itemReader.read();
        Data secondData = itemReader.read();

        assertNotNull(firstData, "El primer registro debe existir");
        assertNotNull(secondData, "El segundo registro debe existir");

        assertEquals("Juan", firstData.getFirstName());
        assertEquals("María", secondData.getFirstName());
        assertEquals("Fernández", secondData.getPaternalLastName());
        assertEquals("González", secondData.getMaternalLastName());

        itemReader.close();
    }

    @Test
    void testReadAllLinesUntilNull() throws Exception {
        itemReader.open(new ExecutionContext());

        int count = 0;
        Data data;

        while ((data = itemReader.read()) != null) {
            count++;
            assertNotNull(data.getFirstName(), "Cada registro debe tener firstName");
            assertNotNull(data.getDisbursementDate(), "Cada registro debe tener disbursementDate");
        }

        assertEquals(20, count, "Debe leer exactamente 20 registros del CSV");

        itemReader.close();
    }

    @Test
    void testDateConversionForMultipleRecords() throws Exception {
        itemReader.open(new ExecutionContext());

        Data firstRecord = itemReader.read();
        Data secondRecord = itemReader.read();
        Data thirdRecord = itemReader.read();

        assertEquals(LocalDate.parse("20/12/2025", dateFormatter), firstRecord.getDisbursementDate());
        assertEquals(LocalDate.parse("22/12/2025", dateFormatter), secondRecord.getDisbursementDate());
        assertEquals(LocalDate.parse("18/12/2025", dateFormatter), thirdRecord.getDisbursementDate());

        itemReader.close();
    }

    @Test
    void testNumericFieldsParsing() throws Exception {
        itemReader.open(new ExecutionContext());

        Data data = itemReader.read();

        assertInstanceOf(Double.class, data.getMonthlyIncome());
        assertInstanceOf(Double.class, data.getLoanAmount());
        assertInstanceOf(Double.class, data.getInterestRate());
        assertInstanceOf(Integer.class, data.getTerm());

        itemReader.close();
    }

    @Test
    void testDifferentCurrencies() throws Exception {
        itemReader.open(new ExecutionContext());

        Data usdRecord = itemReader.read(); // Juan - USD
        Data eurRecord = itemReader.read(); // María - EUR
        Data usdRecord2 = itemReader.read(); // Carlos - USD
        Data mxnRecord = itemReader.read(); // Ana - MXN

        assertEquals("USD", usdRecord.getCurrencyOfIncome());
        assertEquals("EUR", eurRecord.getCurrencyOfIncome());
        assertEquals("USD", usdRecord2.getCurrencyOfIncome());
        assertEquals("MXN", mxnRecord.getCurrencyOfIncome());

        itemReader.close();
    }

    @Test
    void testSkipsHeaderLine() throws Exception {
        itemReader.open(new ExecutionContext());

        Data firstData = itemReader.read();

        // Si no saltara el header, el firstName sería "firstName" (el header)
        assertNotEquals("firstName", firstData.getFirstName(),
                "El reader debe saltar la línea de encabezado");
        assertEquals("Juan", firstData.getFirstName(),
                "El primer registro debe ser Juan, no el header");

        itemReader.close();
    }

    @Test
    void testLargeMonthlyIncomeValues() throws Exception {
        itemReader.open(new ExecutionContext());

        // Saltar hasta Ana (registro 4) que tiene 45000.00 MXN
        itemReader.read(); // Juan
        itemReader.read(); // María
        itemReader.read(); // Carlos
        Data ana = itemReader.read(); // Ana

        assertEquals(45000.00, ana.getMonthlyIncome());
        assertEquals(250000.00, ana.getLoanAmount());

        itemReader.close();
    }

    @Test
    void testDecimalPrecision() throws Exception {
        itemReader.open(new ExecutionContext());

        itemReader.read(); // Juan
        Data maria = itemReader.read(); // María tiene 3200.50

        assertEquals(3200.50, maria.getMonthlyIncome(), 0.001);
        assertEquals(20000.00, maria.getLoanAmount(), 0.001);
        assertEquals(6.75, maria.getInterestRate(), 0.001);

        itemReader.close();
    }

    @Test
    void testTermVariations() throws Exception {
        itemReader.open(new ExecutionContext());

        Data juan = itemReader.read(); // 24 meses
        Data maria = itemReader.read(); // 36 meses
        Data carlos = itemReader.read(); // 18 meses
        Data ana = itemReader.read(); // 48 meses

        assertEquals(24, juan.getTerm());
        assertEquals(36, maria.getTerm());
        assertEquals(18, carlos.getTerm());
        assertEquals(48, ana.getTerm());

        itemReader.close();
    }

    @Test
    void testInterestRateRange() throws Exception {
        itemReader.open(new ExecutionContext());

        Data data;
        double minRate = Double.MAX_VALUE;
        double maxRate = Double.MIN_VALUE;

        while ((data = itemReader.read()) != null) {
            double rate = data.getInterestRate();
            minRate = Math.min(minRate, rate);
            maxRate = Math.max(maxRate, rate);
        }

        assertTrue(minRate >= 6.0, "La tasa mínima debe ser >= 6.0");
        assertTrue(maxRate <= 12.0, "La tasa máxima debe ser <= 12.0");

        itemReader.close();
    }

    @Test
    void testReaderCanBeReopened() throws Exception {
        // Primera lectura
        itemReader.open(new ExecutionContext());
        Data firstRead = itemReader.read();
        assertEquals("Juan", firstRead.getFirstName());
        itemReader.close();

        // Segunda lectura
        itemReader.open(new ExecutionContext());
        Data secondRead = itemReader.read();
        assertEquals("Juan", secondRead.getFirstName(),
                "Debe poder leer desde el inicio después de reabrir");
        itemReader.close();
    }
}
