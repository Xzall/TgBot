package org.example.tgbot.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.example.tgbot.model.Response;
import org.example.tgbot.repository.ResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final ResponseRepository responseRepository;

    public ReportService(ResponseRepository responseRepository) {
        this.responseRepository = responseRepository;
    }

    @Transactional(readOnly = true)
    public CompletableFuture<File> generateReport(Long chatId) {
        log.info("Generating report for chatId: {}", chatId);
        List<Response> completedResponses = responseRepository.findByIsCompletedTrue();
        log.info("Found {} completed responses in database", completedResponses.size());
        for (Response response : completedResponses) {
            log.info("Completed Response [id={}, userId={}, name={}, email={}, rating={}]",
                    response.getId(), response.getUserId(), response.getName(), response.getEmail(), response.getRating());
        }

        XWPFDocument document = new XWPFDocument();

        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        if (header == null) {
            header = table.createRow();
        }
        while (header.getTableCells().size() < 3) {
            header.addNewTableCell();
        }
        header.getCell(0).setText("Имя");
        header.getCell(1).setText("Email");
        header.getCell(2).setText("Оценка");

        if (completedResponses.isEmpty()) {
            XWPFTableRow emptyRow = table.createRow();
            while (emptyRow.getTableCells().size() < 3) {
                emptyRow.addNewTableCell();
            }
            emptyRow.getCell(0).setText("Нет данных");
            emptyRow.getCell(1).setText("-");
            emptyRow.getCell(2).setText("-");
            log.info("No completed responses found, added placeholder row to table");
        } else {
            for (Response response : completedResponses) {
                XWPFTableRow row = table.createRow();
                while (row.getTableCells().size() < 3) {
                    row.addNewTableCell();
                }
                row.getCell(0).setText(response.getName() != null ? response.getName() : "N/A");
                row.getCell(1).setText(response.getEmail() != null ? response.getEmail() : "N/A");
                row.getCell(2).setText(response.getRating() != null ? String.valueOf(response.getRating()) : "N/A");
                log.debug("Added row: name={}, email={}, rating={}", response.getName(), response.getEmail(), response.getRating());
            }
        }

        File tempFile;
        try {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String baseFileName = "Отчёт-" + date + ".docx";
            Path tempDir = Files.createTempDirectory("tg-bot-reports-");
            int counter = 0;
            String fileName = baseFileName;
            while (true) {
                Path filePath = tempDir.resolve(fileName);
                if (!Files.exists(filePath)) {
                    tempFile = filePath.toFile();
                    break;
                }
                counter++;
                fileName = "Отчёт-" + date + "_" + counter + ".docx";
            }
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                document.write(out);
            }
            log.info("Report generated successfully: {}", tempFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error creating report file", e);
            throw new RuntimeException("Ошибка при создании временного файла", e);
        }

        return CompletableFuture.completedFuture(tempFile);
    }
}