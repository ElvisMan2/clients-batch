package com.inetum.clientsbatch.reader;

import com.inetum.clientsbatch.dto.Data;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 Implementación simple: al primer read() carga todos los registros del parquet en memoria,
 los mapea a Data y luego itera retornando uno por uno.
*/
@Component
public class ClientFileReaderParquet {

    @Bean
    public ItemReader<Data> clientParquetItemReader() {
        return new ItemReader<Data>() {
            private Iterator<Data> iterator;
            private boolean initialized = false;

            @Override
            public Data read() throws Exception {
                if (!initialized) {
                    initialized = true;
                    List<Data> list = new ArrayList<>();

                    // Cargar recurso desde classpath
                    ClassPathResource resource = new ClassPathResource("clients.parquet");
                    File file = resource.getFile(); // en ejecución local. Si se empaqueta en JAR puede requerir otro enfoque (FS/HDFS).
                    Path path = new Path(file.getAbsolutePath());

                    // Usar GroupReadSupport para evitar dependencia en parquet-avro
                    try (ParquetReader<Group> reader =
                                 ParquetReader.builder(new GroupReadSupport(), path)
                                         .withConf(new Configuration())
                                         .build()) {

                        Group group;
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                        while ((group = reader.read()) != null) {
                            Data d = new Data();

                            // Para cada campo: obtener índice y comprobar repeticiones antes de leer
                            try {
                                int idx = group.getType().getFieldIndex("firstName");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setFirstName(group.getValueToString(idx, 0));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("paternalLastName");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setPaternalLastName(group.getValueToString(idx, 0));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("maternalLastName");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setMaternalLastName(group.getValueToString(idx, 0));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("currencyOfIncome");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setCurrencyOfIncome(group.getValueToString(idx, 0));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("monthlyIncome");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setMonthlyIncome(Double.parseDouble(group.getValueToString(idx, 0)));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("loanAmount");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setLoanAmount(Double.parseDouble(group.getValueToString(idx, 0)));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("currency");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setCurrency(group.getValueToString(idx, 0));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("interestRate");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setInterestRate(Double.parseDouble(group.getValueToString(idx, 0)));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("term");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setTerm(Integer.parseInt(group.getValueToString(idx, 0)));
                            } catch (Exception ignored) {}

                            try {
                                int idx = group.getType().getFieldIndex("disbursementDate");
                                if (group.getFieldRepetitionCount(idx) > 0)
                                    d.setDisbursementDate(LocalDate.parse(group.getValueToString(idx, 0), formatter));
                            } catch (Exception ignored) {}

                            list.add(d);
                        }
                    }

                    this.iterator = list.iterator();
                }

                if (iterator != null && iterator.hasNext()) {
                    return iterator.next();
                }
                return null;
            }
        };
    }
}
