package org.example.tgbot;

import org.example.tgbot.model.Response;
import org.example.tgbot.repository.ResponseRepository;
import org.example.tgbot.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Тест для проверки многопоточки при формировании отчетов
 */
@ExtendWith(MockitoExtension.class)
public class ReportServiceConcurrencyTest {

    @Mock
    private ResponseRepository responseRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(responseRepository);
    }

    /**
     * Вспомогательный метод для создания тестового ответа
     */
    private Response createResponse(Long userId, String name, String email, Integer rating, boolean completed) {
        Response response = new Response();
        response.setId(1L);
        response.setUserId(userId);
        response.setName(name);
        response.setEmail(email);
        response.setRating(rating);
        response.setCompleted(completed);
        response.setCreatedAt(LocalDateTime.now());
        return response;
    }

    /**
     * Тест проверяет, что при одновременном формировании отчетов разными пользователями
     * не возникает гонок данных и каждый пользователь получает корректный отчет
     */
    @Test
    void generateReport_MultipleConcurrentRequests_AllSucceedWithCorrectData() throws Exception {
        // Количество одновременных запросов на формирование отчета
        int requestCount = 5;
        
        // Создаем пул потоков для имитации одновременных запросов
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        
        // CountDownLatch для синхронизации запуска всех потоков одновременно
        CountDownLatch startLatch = new CountDownLatch(1);
        
        // CountDownLatch для ожидания завершения всех потоков
        CountDownLatch finishLatch = new CountDownLatch(requestCount);
        
        // Счетчик успешных запросов
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Подготавливаем тестовые данные - разные ответы для разных пользователей
        List<Response> completedResponses = new ArrayList<>();
        completedResponses.add(createResponse(123L, "Калыван", "CALLIvan@gmail.com", 8, true));
        completedResponses.add(createResponse(456L, "Чел", "Chhh@gmail.com", 9, true));
        
        // Настраиваем мок репозитория, чтобы он возвращал наши тестовые данные
        when(responseRepository.findByIsCompletedTrue()).thenReturn(completedResponses);
        
        // Список для хранения результатов асинхронных операций
        List<CompletableFuture<File>> futures = new ArrayList<>();
        
        // Создаем и запускаем потоки для имитации одновременных запросов
        for (int i = 0; i < requestCount; i++) {
            final long chatId = 1000L + i; // Уникальный chatId для каждого запроса
            
            executorService.submit(() -> {
                try {
                    // Ждем сигнала для одновременного старта всех потоков
                    startLatch.await();
                    
                    // Запускаем генерацию отчета
                    CompletableFuture<File> future = reportService.generateReport(chatId);
                    futures.add(future);
                    
                    // Ждем результат с таймаутом
                    File resultFile = future.get(5, TimeUnit.SECONDS);
                    
                    // Проверяем результат
                    if (resultFile != null && resultFile.exists() && resultFile.length() > 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Error in thread for chatId " + chatId + ": " + e.getMessage());
                } finally {
                    // Сигнализируем о завершении потока
                    finishLatch.countDown();
                }
            });
        }
        
        // Даем сигнал всем потокам начать выполнение одновременно
        startLatch.countDown();
        
        // Ждем завершения всех потоков
        boolean allFinished = finishLatch.await(10, TimeUnit.SECONDS);
        
        // Завершаем пул потоков
        executorService.shutdown();
        
        // Проверяем результаты
        assertTrue(allFinished, "Не все потоки завершились вовремя");
        assertEquals(requestCount, successCount.get(), "Не все запросы были успешно обработаны");
        
        // Проверяем, что метод findByIsCompletedTrue был вызван requestCount раз
        verify(responseRepository, times(requestCount)).findByIsCompletedTrue();
        
        // Проверяем, что все CompletableFuture завершились успешно
        for (CompletableFuture<File> future : futures) {
            assertTrue(future.isDone(), "CompletableFuture не завершился");
            assertFalse(future.isCompletedExceptionally(), "CompletableFuture завершился с ошибкой");
            
            File file = future.get();
            assertNotNull(file, "Файл отчета не был создан");
            assertTrue(file.exists(), "Файл отчета не существует");
            assertTrue(file.length() > 0, "Файл отчета пустой");
        }
    }
}
