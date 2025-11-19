package com.inetum.clientsbatch.writer;


import com.inetum.clientsbatch.model.Client;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.item.database.JpaItemWriter;

import java.io.FileWriter;
import java.io.IOException;

public class ReportWriter extends JpaItemWriter<Client> {
    public ReportWriter(EntityManagerFactory emf) {
        super.setEntityManagerFactory(emf);
    }

    @AfterStep
    public void afterStep(StepExecution stepExecution) {
        // Custom logic after step completion can be added here
        int readCount = (int) stepExecution.getReadCount();
        int writtenCount = (int) stepExecution.getWriteCount();

        try (FileWriter writer = new FileWriter("report.txt") ) {
            writer.write("Step Summary:\n");
            writer.write("Total Records Read: " + readCount + "\n");
            writer.write("Total Records Written: " + writtenCount + "\n");

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

}
