package com.inetum.clientsbatch.writer;

import com.inetum.clientsbatch.model.Client;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.io.FileWriter;
import java.io.IOException;

public class ReportWriter implements ItemWriter<Client> {

    private int totalWritten = 0;

    @Override
    public void write(Chunk<? extends Client> chunk) throws Exception {
        totalWritten += chunk.getItems().size();
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
