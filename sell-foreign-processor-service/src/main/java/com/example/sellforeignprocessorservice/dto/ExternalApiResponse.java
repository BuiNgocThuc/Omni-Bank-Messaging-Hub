package com.example.sellforeignprocessorservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiResponse<T> {
    private boolean success;
    private String code;
    private T data;
}
