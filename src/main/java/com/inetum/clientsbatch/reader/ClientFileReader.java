package com.inetum.clientsbatch.reader;

import com.inetum.clientsbatch.model.Client;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class ClientFileReader {

    public FlatFileItemReader<Client> clientItemReader() {
        return new FlatFileItemReaderBuilder<Client>()
                .name("clientItemReader")
                .resource(new ClassPathResource("clients.csv"))
                .delimited()
                .names("firstName","paternalLastName","maternalLastName","currencyOfIncome","monthlyIncome")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Client.class);
                }})
                .linesToSkip(1)
                .build();
    }
}

