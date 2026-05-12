package com.example.treasuryservice.client;

import com.example.treasuryservice.dto.FxRatesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class FxRatesClient {

    private final RestClient fxRatesRestClient;

    // gọi qua bên fxrates: currencies tức là target
    public FxRatesResponse fetchRate(String base, String target, int amount) {
        log.info("Calling FxRatesAPI: {} {} → {}", amount, base, target);

        FxRatesResponse response = fxRatesRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/latest")
                        .queryParam("base", base)
                        .queryParam("currencies", target)
                        .queryParam("amount", amount)
                        .build())
                .retrieve()
                .body(FxRatesResponse.class);

        if (response == null || !response.isSuccess()) {
            throw new IllegalStateException("FxRatesAPI returned unsuccessful response");
        }

        if (response.getRates() == null || response.getRates().get(target.toUpperCase()) == null) {
            throw new IllegalStateException(
                    "Rate not found for pair: " + base + " → " + target);
        }

        log.info("FxRatesAPI OK - {} {} → {} {} (rate={})",
                amount, base, target,
                response.getRates().get(target.toUpperCase()),
                response.getRates().get(target.toUpperCase()));

        return response;
    }

    //ấy rate đơn vị (1 unit base = ? unit target)
    public BigDecimal getRate(String base, String target, int amount) {
        FxRatesResponse response = fetchRate(base, target, amount);
        return response.getRates()
                .get(target.toUpperCase())
                .setScale(6, RoundingMode.HALF_UP);
    }
}
