package com.Bd.or.Ballondor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMin;

@ConfigurationProperties(prefix = "football.prediction")
@Validated
public class PredictionProperties {
    @DecimalMin(value = "0.0", inclusive = false)
    private double softmaxTemperature = 12.0;

    public double getSoftmaxTemperature() { return softmaxTemperature; }
    public void setSoftmaxTemperature(double softmaxTemperature) { this.softmaxTemperature = softmaxTemperature; }
}
