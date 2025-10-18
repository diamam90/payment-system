package com.example.individualsapi.filter;

import com.example.individualsapi.annotation.MetricNames;
import com.example.individualsapi.annotation.MetricTags;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class CountAndTimerWebFilter implements WebFilter {

    private final MeterRegistry registry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var context = new ObservationContext(exchange.getRequest(), exchange.getResponse());
        return chain.filter(exchange).tap(() -> new ObservationListener(context, registry));
    }

    @RequiredArgsConstructor
    private static class ObservationListener extends DefaultSignalListener<Void> {

        private final ObservationContext context;
        private final MeterRegistry registry;

        @Override
        public void doFirst() {
            context.setStartMillis(registry.config().clock().wallTime());
        }

        @Override
        public void doOnComplete() {
            var uri = context.getRequest().getURI().getPath();
            var code = context.getResponse().getStatusCode();

            registry.counter(MetricNames.HTTP_REQUEST.getCode(),
                            MetricTags.URI.getCode(), context.request.getURI().getPath(),
                            MetricTags.STATUS.getCode(), String.valueOf(code))
                    .increment();

            registry.timer(MetricNames.HTTP_TIMER_REQUEST.getCode(),
                            MetricTags.URI.getCode(), uri)
                    .record(registry.config().clock().wallTime() - context.startMillis, TimeUnit.MILLISECONDS);
        }
    }

    @Setter
    @Getter
    @RequiredArgsConstructor
    private static class ObservationContext {

        private final ServerHttpRequest request;

        private final ServerHttpResponse response;

        private long startMillis;
    }
}
