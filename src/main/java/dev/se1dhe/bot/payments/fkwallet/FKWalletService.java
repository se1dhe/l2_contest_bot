package dev.se1dhe.bot.payments.fkwallet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.se1dhe.bot.exception.WithdrawException;
import dev.se1dhe.bot.payments.fkwallet.dto.request.WithdrawFkWalletRequest;
import dev.se1dhe.bot.payments.fkwallet.dto.response.CurrenciesResponse;
import dev.se1dhe.bot.payments.fkwallet.dto.response.Currency;
import dev.se1dhe.bot.payments.fkwallet.dto.response.ErrorResponseFK;
import dev.se1dhe.bot.payments.fkwallet.dto.response.PaymentSystem;
import dev.se1dhe.bot.payments.fkwallet.dto.response.PaymentSystemsResponse;
import dev.se1dhe.bot.payments.fkwallet.dto.response.WalletBalanceResponse;
import dev.se1dhe.bot.payments.fkwallet.dto.response.WithdrawFkWalletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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
    @Value("${freekassa.wallet-id}") // This might not be needed for withdrawals if you only use the API key.
    private String WALLET_ID;         // But keep it if you use it elsewhere.
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

        // Формирование URL для запроса баланса.  Используем GET и  PUBLIC_KEY в URL.
        String url = API_URL + PUBLIC_KEY + "/balance";

        //  Подпись для пустого тела, т.к. GET
        String sign = calculateSignature(new HashMap<>());

        // Заголовки.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // JSON, не form-urlencoded!
        headers.set("Authorization", "Bearer " + sign);

        // Тело не нужно для GET-запроса баланса.
        HttpEntity<?> httpRequest = new HttpEntity<>(headers);

        log.info("Balance request URL: {}", url);

        try {
            //  GET-запрос.
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);
            log.info("FKWallet balance response: {}", response.getBody());

            JsonNode rootNode = objectMapper.readTree(response.getBody());

            // FKWallet API returns balance as an array.
            if (rootNode.has("data") && rootNode.get("data").isArray()) {
                for (JsonNode currencyNode : rootNode.get("data")) {
                    if (currencyNode.has("currency_code") && "USDT".equals(currencyNode.get("currency_code").asText())) {
                        return currencyNode.get("value").asText(); // Get the balance as text
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
            throw new RuntimeException("Ошибка при запросе баланса FKWallet", e); // Rethrow with context
        }
    }



    public WithdrawFkWalletResponse createWithdrawal(WithdrawFkWalletRequest request) {
        log.info("Creating withdrawal via FKWallet: {}", request);

        // Подготовка данных для запроса.  Используем LinkedHashMap, чтобы сохранить порядок полей
        Map<String, Object> data = new HashMap<>();
        data.put("currency_id", request.getCurrency());
        data.put("amount", request.getAmount());
        data.put("account", request.getAccount());
        data.put("payment_system_id", request.getPaymentSystemId());
        data.put("fee_from_balance", request.getFeeFromBalance());  // 1 или 0, как в документации
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            data.put("description", request.getDescription());
        }
        if (request.getOrderId() != null) {  //orderId не обязательный
            data.put("order_id", request.getOrderId());
        }
        data.put("idempotence_key", request.getIdempotenceKey());

        // Формирование подписи
        String sign = calculateSignature(data);


        // Добавление подписи в заголовки
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON); // Указываем JSON content type
        headers.set("Authorization", "Bearer " + sign);  // Bearer-токен!
        // Idempotence-Key  уже не нужен, так как передаем в теле

        log.info("Idempotence-Key: {}", request.getIdempotenceKey()); // Логируем, чтобы точно был

        // Создание HTTP-сущности
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(data, headers);

        // Формируем URL
        String url = API_URL + PUBLIC_KEY + "/withdrawal";
        log.info("Withdrawal request URL: {}", url);
        log.info("Request body: {}", data);  // Логируем тело запроса


        try {
            // Выполнение POST-запроса
            ResponseEntity<String> response = restTemplate.exchange(
                    url,           // URL
                    HttpMethod.POST,     // Метод POST
                    httpRequest,      // HTTP-сущность (тело + заголовки)
                    String.class      // Тип ответа - строка (потом распарсим)
            );
            log.info("FKWallet withdrawal response: {}", response.getBody());

            // Десериализация ответа в объект WithdrawFkWalletResponse
            return objectMapper.readValue(response.getBody(), WithdrawFkWalletResponse.class); // Исправлено!

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error during FKWallet withdrawal: {}", e.getResponseBodyAsString(), e);
            try {
                //Используем ErrorResponseFK
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



    // Вспомогательные методы
    private String calculateSignature(Map<String, Object> data) {
        try {
            // Сериализация тела запроса в JSON строку.  Пустой HashMap сериализуется как {}
            String jsonData = data.isEmpty() ? "" : objectMapper.writeValueAsString(data);
            log.info("JSON data for signature: {}", jsonData);


            // Формирование строки для подписи: JSON тела запроса + PRIVATE_KEY
            String dataToSign = jsonData + PRIVATE_KEY;
            log.info("Data to sign: {}", dataToSign);

            // Вычисление SHA-256 хеша
            String sign = sha256(dataToSign);
            log.info("Signature: {}", sign);
            return sign;

        } catch (JsonProcessingException e) {
            log.error("Error serializing data for signature", e);
            throw new RuntimeException("Error serializing data for signature", e);
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


    public String getWalletId() { //Не нужен, если не используете старый способ
        return WALLET_ID;
    }

    public CurrenciesResponse getCurrencies() {
        log.info("Запрос списка доступных валют FKWallet");

        // Формирование URL для запроса списка валют
        String url = API_URL + PUBLIC_KEY + "/currencies";

        // Формирование подписи для пустого тела запроса
        String sign = calculateSignature(new HashMap<>());

        // Подготовка заголовков
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + sign);

        // Создание HTTP-сущности с заголовками (тело не нужно для GET-запроса)
        HttpEntity<?> httpRequest = new HttpEntity<>(headers);

        log.info("Currencies request URL: {}", url);

        try {
            // Отправка GET-запроса и получение ответа в виде строки
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);
            log.info("FKWallet currencies response: {}", response.getBody());

            // Преобразование JSON-ответа в объект CurrenciesResponse
            return objectMapper.readValue(response.getBody(), CurrenciesResponse.class); // Исправлено!
        } catch (Exception e) {
            log.error("Ошибка при запросе списка доступных валют FKWallet", e);
            throw new RuntimeException("Ошибка при запросе списка доступных валют FKWallet", e);
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

    public BigDecimal getExchangeRate(String currencyCode) {
        try {
            CurrenciesResponse currenciesResponse = getCurrencies();
            if (currenciesResponse != null && "ok".equals(currenciesResponse.getStatus())) {
                //ищем USDT
                for (Currency currency : currenciesResponse.getData()) {
                    if (currencyCode.equals(currency.getCode())) {
                        return currency.getCourse();
                    }
                }
                log.error("Не удалось найти курс {} в ответе от FKWallet", currencyCode);
                throw new RuntimeException("Не удалось найти курс " + currencyCode + " в ответе от FKWallet");
            } else {
                log.error("Ошибка при получении курсов валют: {}", currenciesResponse != null ? currenciesResponse.getStatus() : "null response");
                throw new RuntimeException("Ошибка при получении курсов валют от FKWallet");
            }
        } catch (Exception e) {
            log.error("Ошибка при обращении к API FKWallet для получения курса", e);
            throw new RuntimeException("Ошибка при обращении к API FKWallet для получения курса", e);
        }
    }
}