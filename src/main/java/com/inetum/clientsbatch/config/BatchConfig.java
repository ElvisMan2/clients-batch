package com.inetum.clientsbatch.config;

import com.inetum.clientsbatch.dto.Data;
import com.inetum.clientsbatch.processor.ClientItemProcessor;
import com.inetum.clientsbatch.reader.ClientFileReader;
import com.inetum.clientsbatch.writer.ReportWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    private final ClientFileReader clientFileReader;

    public BatchConfig(ClientFileReader clientFileReader) {
        this.clientFileReader = clientFileReader;
    }

    @Bean
    ClientItemProcessor processor() {
        return new ClientItemProcessor();
    }

    @Bean
    ReportWriter writer() {
        return new ReportWriter();
    }

    @Bean
    public Step step(JobRepository jobRepository,
                     PlatformTransactionManager platformTransactionManager){
        return new StepBuilder("step1",jobRepository)
                .<Data, Data>chunk(5,platformTransactionManager)
                .reader(clientFileReader.clientItemReader())//extrae los clientes del csv
                .processor(processor())//procesa cada cliente enviandolo a la api
                .writer(writer())//genera el reporte
                .build();
    }

    @Bean
    public Job job(JobRepository jobRepository, Step step) {
        return new JobBuilder("importClientsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }
}
