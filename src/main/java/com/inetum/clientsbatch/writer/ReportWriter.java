package com.inetum.clientsbatch.writer;

import com.inetum.clientsbatch.dto.Data;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportWriter implements ItemWriter<Data> {

    private int totalRead = 0;
    private int totalApproved = 0;
    private final List<Data> approvedLoans = new ArrayList<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void write(Chunk<? extends Data> chunk) throws Exception {
        for (Data data : chunk.getItems()) {
            totalRead++;
            // Solo considerar préstamos aprobados (que tienen loanId)
            if (data.getLoanId() != null) {
                totalApproved++;
                approvedLoans.add(data);
            }
        }
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        try (FileWriter writer = new FileWriter("report.txt")) {
            writer.write("=".repeat(120) + "\n");
            writer.write("REPORTE DE PRÉSTAMOS GENERADOS\n");
            writer.write("=".repeat(120) + "\n\n");

            writer.write(String.format("Total de registros procesados: %d\n", totalRead));
            writer.write(String.format("Total de préstamos generados: %d\n", totalApproved));
            writer.write(String.format("Simulaciones no aprobadas: %d\n\n", totalRead - totalApproved));

            writer.write("=".repeat(120) + "\n");
            writer.write(String.format("%-10s %-15s %-20s %-20s %-10s %-10s %-15s %-15s %-15s %-15s %-10s %-12s\n",
                    "ID Cliente", "Nombre", "Apellido Pat.", "Apellido Mat.", "ID Préstamo", "Moneda",
                    "Monto Préstamo", "Total Interés", "Fecha Desemb.", "Próximo Pago", "Cuotas", "Monto Cuota"));
            writer.write("=".repeat(120) + "\n");

            for (Data loan : approvedLoans) {
                writer.write(String.format("%-10d %-15s %-20s %-20s %-10d %-10s %-15.2f %-15.2f %-15s %-15s %-10d %-12.2f\n",
                        loan.getClientId(),
                        loan.getFirstName(),
                        loan.getPaternalLastName(),
                        loan.getMaternalLastName(),
                        loan.getLoanId(),
                        loan.getCurrency(),
                        loan.getLoanAmount(),
                        loan.getTotalInterest(),
                        loan.getDisbursementDate().format(dateFormatter),
                        loan.getNextPaymentDate().format(dateFormatter),
                        loan.getTerm(),
                        loan.getMonthlyPayment()));
            }

            writer.write("=".repeat(120) + "\n");

            System.out.println("\n✓ Reporte generado exitosamente: report.txt");
            System.out.println("Total de préstamos generados: " + totalApproved);

        } catch (IOException e) {
            System.err.println("✗ Error al generar el reporte");
            e.printStackTrace();
        }
    }
}
