package dev.se1dhe.bot.payments.fkwallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.se1dhe.bot.exception.WithdrawException;
import dev.se1dhe.bot.payments.fkwallet.dto.request.WithdrawFkWalletRequest;
import dev.se1dhe.bot.payments.fkwallet.dto.response.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class FKWalletService {

    private final String API_URL = "https://api.fkwallet.io/v1/";

    @Value("${fkwallet.public-key}")
    private String PUBLIC_KEY;
    @Value("${freekassa.wallet-id}")
    private String WALLET_ID;
    @Value("${fkwallet.private-key}")
    private String PRIVATE_KEY;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public FKWalletService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public String getWalletBalance() {
        log.info("Запрос баланса FKWallet");

        String url = API_URL + PUBLIC_KEY + "/balance";


        String sign = calculateSignature(new HashMap<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + sign);

        HttpEntity<?> httpRequest = new HttpEntity<>(headers);

        log.info("Balance request URL: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);
            log.info("FKWallet balance response: {}", response.getBody());

            JsonNode rootNode = objectMapper.readTree(response.getBody());

            if (rootNode.has("data") && rootNode.get("data").isArray()) {
                for (JsonNode currencyNode : rootNode.get("data")) {
                    if (currencyNode.has("currency_code") && "USDT".equals(currencyNode.get("currency_code").asText())) {
                        return currencyNode.get("value").asText();
                    }
                }
                log.error("Ответ не содержит баланс в валюте USDT");
                throw new RuntimeException("Ответ не содержит баланс в валюте USDT");
            } else {
                log.error("Ответ не содержит поле 'data' или оно имеет неверную структуру");
                throw new RuntimeException("Ответ не содержит поле 'data' или оно имеет неверную структуру");
            }

        } catch (Exception e) {
            log.error("Ошибка при запросе баланса FKWallet", e);
            throw new RuntimeException("Ошибка при запросе баланса FKWallet", e);
        }
    }

    public WithdrawFkWalletResponse createWithdrawal(WithdrawFkWalletRequest request) {
        log.info("Creating withdrawal via FKWallet: {}", request);

        Map<String, Object> data = new HashMap<>();
        data.put("currency_id", request.getCurrency());
        data.put("amount", request.getAmount());
        data.put("account", request.getAccount());
        data.put("payment_system_id", request.getPaymentSystemId());
        data.put("fee_from_balance", request.getFeeFromBalance());
        data.put("idempotence_key", request.getIdempotenceKey()); // Добавляем в тело
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            data.put("description", request.getDescription());
        }
        if (request.getOrderId() != null) {
            data.put("order_id", request.getOrderId());
        }

        String sign = calculateSignature(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + sign);

        log.info("Idempotence-Key: {}", request.getIdempotenceKey());

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(data, headers);

        String url = API_URL + PUBLIC_KEY + "/withdrawal";
        log.info("Withdrawal request URL: {}", url);
        log.info("Request body: {}", data);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    String.class
            );
            log.info("FKWallet withdrawal response: {}", response.getBody());

            return objectMapper.readValue(response.getBody(), WithdrawFkWalletResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error during FKWallet withdrawal: {}", e.getResponseBodyAsString(), e);
            try {
                ErrorResponseFK errorResponse = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponseFK.class);
                throw new WithdrawException(errorResponse.getMessage());
            } catch (JsonProcessingException ex) {
                log.error("Error parsing error response", ex);
                throw new RuntimeException("Failed to parse error response", ex);
            }
        } catch (Exception e) {
            log.error("Error during FKWallet withdrawal", e);
            throw new RuntimeException("Error during FKWallet withdrawal", e);
        }
    }


    public CurrenciesResponse getCurrencies() {
        log.info("Запрос списка доступных валют FKWallet");

        String url = API_URL + PUBLIC_KEY + "/currencies";

        String sign = calculateSignature(new HashMap<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + sign);

        HttpEntity<?> httpRequest = new HttpEntity<>(headers);

        log.info("Currencies request URL: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);
            log.info("FKWallet currencies response: {}", response.getBody());

            return objectMapper.readValue(response.getBody(), CurrenciesResponse.class); // Исправлено!
        } catch (Exception e) {
            log.error("Ошибка при запросе списка доступных валют FKWallet", e);
            throw new RuntimeException("Ошибка при запросе списка доступных валют FKWallet", e);
        }
    }


    private String calculateSignature(Map<String, Object> data) {
        try {
            String jsonData = data.isEmpty() ? "" : objectMapper.writeValueAsString(data);
            log.info("JSON data for signature: {}", jsonData);

            String dataToSign = jsonData + PRIVATE_KEY;
            log.info("Data to sign: {}", dataToSign);

            String sign = sha256(dataToSign);
            log.info("Signature: {}", sign);

            return sign;
        } catch (JsonProcessingException e) {
            log.error("Error serializing data for signature", e);
            throw new RuntimeException("Error serializing data for signature", e);
        }
    }
    public List<PaymentSystem> getPaymentSystems() {
        log.info("Запрос списка платежных систем FKWallet");

        String url = API_URL + PUBLIC_KEY + "/payment_systems";

        String sign = calculateSignature(new HashMap<>());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + sign);

        HttpEntity<?> httpRequest = new HttpEntity<>(headers);

        log.info("Payment systems request URL: {}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);
            log.info("FKWallet payment systems response: {}", response.getBody());

            PaymentSystemsResponse paymentSystemsResponse = objectMapper.readValue(response.getBody(), PaymentSystemsResponse.class);

            if ("success".equals(paymentSystemsResponse.getStatus())) {
                return paymentSystemsResponse.getData();
            } else {
                log.error("Ошибка в ответе от FKWallet: {}", response.getBody());
                throw new RuntimeException("Ошибка при получении списка платежных систем от FKWallet");
            }
        } catch (Exception e) {
            log.error("Ошибка при запросе списка платежных систем FKWallet", e);
            throw new RuntimeException("Ошибка при запросе списка платежных систем FKWallet", e);
        }
    }
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException(e);
        }
    }

    public String getWalletId() {
        return WALLET_ID;
    }
}