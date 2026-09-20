package com.example.service.impl;

import com.example.entity.Payout;
import com.example.entity.Status;
import com.example.entity.Transaction;
import com.example.exception.BadRequestException;
import com.example.fake.dto.StatusUpdate;
import com.example.repository.WebhookRepository;
import com.example.service.PayoutService;
import com.example.service.TransactionService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

    @Mock
    TransactionService transactionService;
    @Mock
    PayoutService payoutService;
    @Mock
    WebhookRepository webhookRepository;

    @Spy
    ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    WebhookServiceImpl webhookService;

    ExecutorService executorService = Executors.newFixedThreadPool(10);

    @RepeatedTest(100)
    void updatePayout() {
        // given
        Payout payout = new Payout();
        payout.setStatus(Status.PENDING);
        payout.setId(228L);

        StatusUpdate success = requestSuccess();
        StatusUpdate failed = requestFailed();

        // when
        when(payoutService.findByIdPessimisticWrite(228L)).thenReturn(Optional.of(payout));

        List<Future<?>> taskFutures = new ArrayList<>();
        IntStream.rangeClosed(1, 5)
                .forEach(_ -> {
                    taskFutures.add(executorService.submit(() -> webhookService.updatePayout(success)));
                    taskFutures.add(executorService.submit(() -> webhookService.updatePayout(failed)));
                });
        AtomicInteger badRequestCounts = new AtomicInteger();
        taskFutures.forEach(future -> {
            try {
                future.get();
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof BadRequestException) {
                    badRequestCounts.getAndIncrement();
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException();
            }
        });

        // then
        assertThat(badRequestCounts).hasValue(5);
        verify(webhookRepository).save(any());
    }

    @RepeatedTest(100)
    void updateTransaction() {
        // given
        Transaction transaction = new Transaction();
        transaction.setStatus(Status.PENDING);
        transaction.setId(228L);

        StatusUpdate success = requestSuccess();
        StatusUpdate failed = requestFailed();

        // when
        when(transactionService.findByIdPessimisticWrite(228L)).thenReturn(Optional.of(transaction));

        List<Future<?>> taskFutures = new ArrayList<>();
        IntStream.rangeClosed(1, 5)
                .forEach(_ -> {
                    taskFutures.add(executorService.submit(() -> webhookService.updateTransaction(success)));
                    taskFutures.add(executorService.submit(() -> webhookService.updateTransaction(failed)));
                });
        AtomicInteger barRequestCounts = new AtomicInteger();
        taskFutures.forEach(future -> {
            try {
                future.get();
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof BadRequestException) {
                    barRequestCounts.getAndIncrement();
                }
            } catch (InterruptedException ex) {
                throw new RuntimeException();
            }
        });

        // then
        assertThat(barRequestCounts).hasValue(5);
        verify(webhookRepository).save(any());
    }

    private static StatusUpdate requestFailed() {
        StatusUpdate request = new StatusUpdate();
        request.setId(228L);
        request.setStatus(StatusUpdate.StatusEnum.FAILED);

        return request;
    }

    private static StatusUpdate requestSuccess() {
        StatusUpdate request = new StatusUpdate();
        request.setId(228L);
        request.setStatus(StatusUpdate.StatusEnum.SUCCESS);

        return request;
    }
}