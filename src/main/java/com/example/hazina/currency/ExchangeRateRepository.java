package com.example.hazina.currency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    List<ExchangeRate> findByBaseCurrencyAndQuoteCurrencyOrderByEffectiveDateDesc(
            String baseCurrency, String quoteCurrency);

    Optional<ExchangeRate> findTopByBaseCurrencyAndQuoteCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String baseCurrency, String quoteCurrency, LocalDate effectiveDate);
}
