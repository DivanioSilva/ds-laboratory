package com.example.demo.config;

import com.example.demo.batch.PersonCsvRecord;
import com.example.demo.model.Person;
import com.example.demo.repository.PersonRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class PersonImportBatchConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<PersonCsvRecord> personCsvReader(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemReaderBuilder<PersonCsvRecord>()
                .name("personCsvReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip(1)
                .delimited(delimited -> delimited.names("firstName", "lastName", "age"))
                .targetType(PersonCsvRecord.class)
                .build();
    }

    @Bean
    public ItemProcessor<PersonCsvRecord, Person> personCsvProcessor(
            PersonRepository personRepository) {
        return record -> {
            validate(record);

            boolean alreadyExists = personRepository
                    .existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndAge(
                            record.getFirstName(), record.getLastName(), record.getAge());

            if (alreadyExists) {
                return null;
            }

            return new Person(null, record.getFirstName(), record.getLastName(), record.getAge());
        };
    }

    @Bean
    public ItemWriter<Person> personDatabaseWriter(PersonRepository personRepository) {
        return chunk -> personRepository.saveAll(chunk.getItems());
    }

    @Bean
    public Step importPersonsStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<PersonCsvRecord> personCsvReader,
            ItemProcessor<PersonCsvRecord, Person> personCsvProcessor,
            ItemWriter<Person> personDatabaseWriter) {
        return new StepBuilder("importPersonsStep", jobRepository)
                .<PersonCsvRecord, Person>chunk(10)
                .transactionManager(transactionManager)
                .reader(personCsvReader)
                .processor(personCsvProcessor)
                .writer(personDatabaseWriter)
                .build();
    }

    @Bean
    public Job importPersonsJob(JobRepository jobRepository, Step importPersonsStep) {
        return new JobBuilder("importPersonsJob", jobRepository)
                .start(importPersonsStep)
                .build();
    }

    private void validate(PersonCsvRecord record) {
        if (record.getFirstName() == null || record.getFirstName().isBlank()) {
            throw new IllegalArgumentException("O primeiro nome é obrigatório no CSV");
        }
        if (record.getLastName() == null || record.getLastName().isBlank()) {
            throw new IllegalArgumentException("O apelido é obrigatório no CSV");
        }
        if (record.getAge() == null || record.getAge() < 1) {
            throw new IllegalArgumentException("A idade deve ser maior do que zero no CSV");
        }
    }
}
