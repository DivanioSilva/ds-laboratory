package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.example.demo.dto.PersonImportResponse;
import com.example.demo.exception.PersonImportException;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/persons")
public class PersonImportController {

    private final JobOperator jobOperator;
    private final Job importPersonsJob;

    public PersonImportController(JobOperator jobOperator, Job importPersonsJob) {
        this.jobOperator = jobOperator;
        this.importPersonsJob = importPersonsJob;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PersonImportResponse importCsv(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro CSV é obrigatório");
        }

        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("person-import-", ".csv");
            file.transferTo(temporaryFile);

            JobParameters parameters = new JobParametersBuilder()
                    .addString("filePath", temporaryFile.toAbsolutePath().toString())
                    .addString("requestId", UUID.randomUUID().toString())
                    .toJobParameters();
            JobExecution execution = jobOperator.start(importPersonsJob, parameters);

            if (execution.getStatus() != BatchStatus.COMPLETED) {
                String reason = execution.getAllFailureExceptions().stream()
                        .findFirst()
                        .map(Throwable::getMessage)
                        .orElse("O processamento do ficheiro não foi concluído");
                throw new PersonImportException(reason);
            }

            long read = execution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getReadCount)
                    .sum();
            long written = execution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount)
                    .sum();
            long ignored = execution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getFilterCount)
                    .sum();

            return new PersonImportResponse(execution.getStatus().name(), read, written, ignored);
        } catch (PersonImportException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PersonImportException("Não foi possível importar o ficheiro CSV", exception);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // O sistema operativo removerá os ficheiros temporários remanescentes.
        }
    }
}
