package com.inetum.clientsbatch.config;

import com.inetum.clientsbatch.model.Client;
import com.inetum.clientsbatch.processor.ClientItemProcessor;
import com.inetum.clientsbatch.writer.ReportWriter;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    private final EntityManagerFactory emf;

    public BatchConfig(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Bean
    public FlatFileItemReader<Client>  reader() {
        return new FlatFileItemReaderBuilder<Client>()
                .name("clientItemReader")
                .resource(new ClassPathResource("clients.csv"))
                .delimited()
                .names(new String[]{"name", "lastName", "email"})
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Client>() {{
                    setTargetType(Client.class);
                }})
                .linesToSkip(1)
                .build();
    }

    @Bean
    ClientItemProcessor processor() {
        return new ClientItemProcessor();
    }

    @Bean
    ReportWriter writer() {
        return new ReportWriter(emf);
    }

    @Bean
    public Step step(JobRepository jobRepository,
                     PlatformTransactionManager platformTransactionManager){
        return new StepBuilder("step1",jobRepository)
                .<Client, Client>chunk(5,platformTransactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer())
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
