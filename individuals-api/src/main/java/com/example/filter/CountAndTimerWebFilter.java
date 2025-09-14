package com.example.filter;

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

            registry.counter("custom_web_request_counter",
                            "uri", context.request.getURI().getPath(),
                            "status", String.valueOf(code))
                    .increment();

            registry.timer("custom_web_request_timer",
                            "uri", uri)
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
