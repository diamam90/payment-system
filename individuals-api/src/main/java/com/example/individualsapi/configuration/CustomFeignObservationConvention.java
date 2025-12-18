package com.example.individualsapi.configuration;

import feign.micrometer.DefaultFeignObservationConvention;

public class CustomFeignObservationConvention extends DefaultFeignObservationConvention {

    @Override
    public String getName() {
        return "http.feignclient.requests";
    }
}
