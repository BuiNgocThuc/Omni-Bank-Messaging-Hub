package com.example.sellforeignservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class SellForeignTransactionResponse {

//    @JsonProperty("tx_id")
//    private String txId;

    private String message;
}
