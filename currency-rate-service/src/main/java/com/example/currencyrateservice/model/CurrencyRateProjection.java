package com.example.currencyrateservice.model;

import java.math.BigDecimal;
import java.time.Instant;

public interface CurrencyRateProjection {

    Long getId();

    String getSourceCode();

    String getDestinationCode();

    Instant getRateBeginTime();

    Instant getRateEndTime();

    BigDecimal getRate();

    String getProviderCode();

    Integer getProviderPriority();
}
