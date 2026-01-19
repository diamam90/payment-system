package com.example.individualsapi.annotation;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Aspect
@RequiredArgsConstructor
public class RequestCounterAspect {

    private final MeterRegistry registry;

    @Pointcut("@annotation(requestCounter)")
    private void invoke(RequestCounter requestCounter) {
    }

    @Around("invoke(counter)")
    public Object count(ProceedingJoinPoint pjp, RequestCounter counter) throws Throwable {
        String tag = counter.metric().getCode();

        try {
            Object result = pjp.proceed();
            if (result instanceof Mono<?> monoResult) {
                return monoResult.doOnSuccess((_) -> increment(tag, true))
                        .doOnError((_) -> increment(tag, false));
            }

            increment(tag, true);
            return result;
        } catch (Throwable thr) {
            increment(tag, false);
            throw thr;
        }
    }


    private void increment(String methodName, boolean isSuccess) {
        var counter = registry.counter(methodName, "status", isSuccess ? "success" : "fail");
        counter.increment();
    }
}
