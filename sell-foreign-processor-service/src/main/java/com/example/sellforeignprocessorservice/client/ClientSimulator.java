package com.example.sellforeignprocessorservice.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ClientSimulator {

    private static final String BASE_URL = "http://localhost:8082/api/v1/processor/transactions/";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java ClientSimulator <txId>");
            return;
        }

        String txId = args[0];
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + txId))
                .GET()
                .build();

        System.out.println("Polling transaction: " + txId);

        for (int i = 0; i < 15; i++) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            System.out.println("[Attempt " + (i + 1) + "] HTTP " + response.statusCode() + " → " + body);

            if (body.contains("SUCCESS") || body.contains("FAILED")) {
                System.out.println("Done.");
                return;
            }

            Thread.sleep(2000);
        }

        System.out.println("Timeout after 30s.");
    }
}
