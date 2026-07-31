package com.example.currencyrateservice.IT;

import com.example.currate.api.CurrateApi;
import com.example.currate.dto.CurrencyResponse;
import com.example.currencyrateservice.client.CurrateClient;
import com.example.currencyrateservice.config.MeterRegistryConfig;
import com.example.currencyrateservice.config.TestSecurityConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DirtiesContext
@SpringBootTest
@Import({TestSecurityConfig.class, MeterRegistryConfig.class})
public class CurrateClientIT {

    @MockitoSpyBean
    CurrateClient currateClient;
    @MockitoBean
    CurrateApi api;
    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;
    @Autowired
    RateLimiterRegistry rateLimiterRegistry;

    @Value("${resilience4j.ratelimiter.instances.currate.limit-for-period}")
    private Integer limitForPeriod;

    @BeforeEach
    void reset() {
        circuitBreakerRegistry.find("currate").ifPresent(CircuitBreaker::reset);
        // refresh rateLimiterPeriod
        while (rateLimiterRegistry.rateLimiter("currate").getMetrics().getAvailablePermissions() < limitForPeriod) {
        }
    }


    @Test
    void getActiveCurrencies_whenAttemptFailed_shouldRetry3Times() {
        when(api.actualData(any()))
                .thenThrow(new HttpServerErrorException(HttpStatusCode.valueOf(503)));
        assertThatThrownBy(() -> currateClient.getActiveCurrencies()).isExactlyInstanceOf(HttpServerErrorException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatusCode.valueOf(503));
        verify(api, times(3)).actualData(any());
    }

    @Test
    void getActiveCurrencies_when9AttemptsFailed_shouldOpenCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.find("currate").orElseThrow();

        assertThat(circuitBreaker)
                .hasFieldOrPropertyWithValue("state", CircuitBreaker.State.CLOSED);

        when(api.actualData(any()))
                .thenThrow(new HttpServerErrorException(HttpStatusCode.valueOf(503)));
        // should execute 3 x 3(retry) attempts
        IntStream.rangeClosed(1, 3)
                .forEach(_ -> assertThatThrownBy(() -> currateClient.getActiveCurrencies()).isExactlyInstanceOf(HttpServerErrorException.class));
        assertThat(circuitBreaker)
                .hasFieldOrPropertyWithValue("state", CircuitBreaker.State.OPEN);

        // should not execute api
        IntStream.rangeClosed(1, 5)
                .forEach(_ -> assertThatThrownBy(() -> currateClient.getActiveCurrencies()).isExactlyInstanceOf(CallNotPermittedException.class));

        verify(api, times(9)).actualData(any());
    }

    @Test
    void getActiveCurrency_when6Attempts_shouldThrowRequestNotPermitted() {
        assertThat(rateLimiterRegistry.rateLimiter("currate").getMetrics().getAvailablePermissions()).isEqualTo(5);
        CurrencyResponse response = new CurrencyResponse();
        response.setStatus(200);
        response.setData(List.of("ABCCDA"));
        when(api.actualData(any()))
                .thenReturn(ResponseEntity.ok(response));
        int start = LocalTime.now().toSecondOfDay();
        IntStream.rangeClosed(1, 5)
                .forEach(i -> currateClient.getActiveCurrencies());
        // next call should throw ex
        assertThatThrownBy(() -> currateClient.getActiveCurrencies()).isExactlyInstanceOf(RequestNotPermitted.class);
        int end = LocalTime.now().toSecondOfDay();
        assertThat(end - start).isEqualTo(0);
    }

    @Test
    void getActiveCurrency_when5ParallelAttempts_shouldThrowBulkheadFullException() throws InterruptedException {
        CurrencyResponse response = new CurrencyResponse();
        response.setStatus(200);
        response.setData(List.of("ABCCDA"));
        when(api.actualData(any()))
                .thenReturn(ResponseEntity.ok(response));

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);
        AtomicInteger failCount = new AtomicInteger();
        IntStream.rangeClosed(1, 5)
                .forEach(_ -> executorService.execute(() -> {
                    try {
                        currateClient.getActiveCurrencies();
                    } catch (Exception ex) {
                        assertThat(ex).isExactlyInstanceOf(BulkheadFullException.class);
                        failCount.getAndIncrement();
                    } finally {
                        latch.countDown();
                    }
                }));
        latch.await();
        executorService.shutdown();

        assertThat(failCount).hasValue(2);
        verify(api, times(3)).actualData(any());
    }
}


