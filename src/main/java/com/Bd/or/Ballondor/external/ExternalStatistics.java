package com.Bd.or.Ballondor.external;

import java.math.BigDecimal;

public record ExternalStatistics(Integer appearances, Integer goals, Integer assists, Integer minutes,
                                 BigDecimal rating, Integer shots, Integer shotsOnTarget, Integer keyPasses,
                                 Integer passAccuracy, Integer tackles, Integer interceptions, Integer saves,
                                 Integer goalsConceded) {}
