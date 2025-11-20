package com.inetum.clientsbatch.reader;

import com.inetum.clientsbatch.dto.Data;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class ClientFileReader {

    public FlatFileItemReader<Data> clientItemReader() {

        BeanWrapperFieldSetMapper<Data> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Data.class);

        // ⭐ Registro del editor para convertir dd/MM/yyyy → LocalDate
        mapper.setCustomEditors(Map.of(
                LocalDate.class,
                new PropertyEditorSupport() {
                    @Override
                    public void setAsText(String text) throws IllegalArgumentException {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        setValue(LocalDate.parse(text, formatter));
                    }
                }
        ));

        return new FlatFileItemReaderBuilder<Data>()
                .name("clientItemReader")
                .resource(new ClassPathResource("clients.csv"))
                .delimited()
                .names(
                        "firstName",
                        "paternalLastName",
                        "maternalLastName",
                        "currencyOfIncome",
                        "monthlyIncome",
                        "loanAmount",
                        "currency",
                        "interestRate",
                        "term",
                        "disbursementDate"
                )
                .fieldSetMapper(mapper)   // ⭐ con editor ya configurado
                .linesToSkip(1)
                .build();
    }
}

