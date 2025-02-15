package dev.se1dhe.bot.payments.freekassa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.se1dhe.bot.exception.WithdrawException;
import dev.se1dhe.bot.payments.freekassa.dto.Notification;
import dev.se1dhe.bot.payments.freekassa.dto.request.PaymentRequest;
import dev.se1dhe.bot.payments.freekassa.dto.request.WithdrawRequest;
import dev.se1dhe.bot.payments.freekassa.dto.response.ErrorResponse;
import dev.se1dhe.bot.payments.freekassa.dto.response.WithdrawResponse;
import dev.se1dhe.bot.service.BalanceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Log4j2
public class FreeKassaService {

    private final String API_URL = "https://api.fk.life/v1/ ";
    private final String PAYMENT_URL = "https://pay.fk.money/";

    @Value("${freekassa.merchant-id}")
    private int MERCHANT_ID;
    @Value("${freekassa.secret1}")
    private String SECRET1;
    @Value("${freekassa.secret2}")
    private String SECRET2;
    @Value("${freekassa.api.key}")
    private String API_KEY;

    private final RestTemplate restTemplate;
    private final BalanceService balanceService;
    private final ObjectMapper objectMapper;

    @Autowired
    public FreeKassaService(RestTemplate restTemplate, BalanceService balanceService) {
        this.restTemplate = restTemplate;
        this.balanceService = balanceService;
        this.objectMapper = new ObjectMapper();
    }

    public String createPayment(PaymentRequest request) {
        // Формирование подписи
        request.setS(calculateSign(request.getM(), request.getOa().toPlainString(), request.getCurrency(), request.getO()));

        // Построение URL
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(PAYMENT_URL)
                .queryParam("m", request.getM())
                .queryParam("oa", request.getOa())
                .queryParam("o", request.getO())
                .queryParam("s", request.getS());

        if (request.getCurrency() != null) {
            builder.queryParam("currency", request.getCurrency());
        }
        if (request.getI() != null) {
            builder.queryParam("i", request.getI());
        }
        if (request.getPhone() != null) {
            builder.queryParam("phone", request.getPhone());
        }
        if (request.getEm() != null) {
            builder.queryParam("em", request.getEm());
        }
        if (request.getLang() != null) {
            builder.queryParam("lang", request.getLang());
        }
        if (request.getUs() != null && !request.getUs().isEmpty()) {
            request.getUs().forEach((k, v) -> {
                builder.queryParam("us_" + k, v);
            });
        }

        String paymentUrl = builder.build().toUriString();
        log.info("Generated payment URL: {}", paymentUrl);
        return paymentUrl;
    }

    public String handlePaymentNotification(Notification notification) {
        // Проверка подписи
        String expectedSign = calculateNotificationSign(notification);
        if (!expectedSign.equals(notification.getSIGN())) {
            log.error("Invalid signature in notification: {}", notification);
            return "ERROR: Invalid signature";
        }

        // TODO: Добавить обработку уведомления (например, обновление статуса заказа в вашей системе)

        // Пополнение баланса пользователя
        balanceService.deposit(Long.valueOf(notification.getMERCHANT_ORDER_ID()), notification.getAMOUNT());

        // Возвращаем YES, чтобы FreeKassa знала, что уведомление обработано
        return "YES";
    }

    public WithdrawResponse createWithdrawal(WithdrawRequest request) {
        log.info("Creating withdrawal: {}", request);

        // Генерация nonce, если не предоставлен
        request.setNonce(String.valueOf(Instant.now().getEpochSecond()));

        // Формирование подписи
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shopId", request.getShopId());
        data.put("nonce", request.getNonce());
        data.put("i", request.getI());
        data.put("account", request.getAccount());
        data.put("amount", request.getAmount());
        if (request.getCurrency() != null) {
            data.put("currency", request.getCurrency());
        }

        // Добавляем signature последним параметром
        request.setSignature(calculateApiSign(data));
        data.put("signature", request.getSignature());

        // Подготовка и отправка запроса
        String url = API_URL + "payouts";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(data, headers);

        // Логирование URL с параметрами
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        data.forEach(builder::queryParam);
        String urlWithParams = builder.toUriString();
        log.info("Withdrawal request URL: {}", urlWithParams);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, String.class);
            log.info("Withdrawal response: {}", response.getBody());

            // Десериализация ответа в объект WithdrawResponse
            return objectMapper.readValue(response.getBody(), WithdrawResponse.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP error during withdrawal: {}", e.getResponseBodyAsString(), e);
            // Обработка ошибок на основе содержимого ответа
            try {
                ErrorResponse errorResponse = objectMapper.readValue(e.getResponseBodyAsString(), ErrorResponse.class);
                throw new WithdrawException(errorResponse.getMessage());
            } catch (JsonProcessingException ex) {
                log.error("Error parsing error response", ex);
                throw new RuntimeException("Failed to parse error response", ex);
            }
        } catch (Exception e) {
            log.error("Error during withdrawal", e);
            throw new RuntimeException("Error during withdrawal", e);
        }
    }

    public String getFreekassaBalance() {
        log.info("Запрос баланса FreeKassa");

        String nonce = String.valueOf(Instant.now().getEpochSecond());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("shopId", MERCHANT_ID);
        data.put("nonce", nonce);
        data.put("signature", calculateApiSign(data));

        // Формирование URL для запроса баланса
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_URL + "balance");
        data.forEach(builder::queryParam);
        String url = builder.toUriString();

        log.info("Balance request URL: {}", url);

        // Подготовка заголовков
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Создание HTTP-сущности с заголовками (тело не нужно для GET)
        HttpEntity<?> httpRequest = new HttpEntity<>(headers);

        try {
            // Отправка GET-запроса и получение ответа в виде строки
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpRequest, String.class);
            log.info("FreeKassa balance response: {}", response.getBody());

            // Преобразование JSON-ответа в JsonNode
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            // Проверка наличия поля "balance" и извлечение его значения, если оно есть
            if (rootNode.has("balance")) {
                return rootNode.get("balance").toString();
            } else {
                log.error("Ответ не содержит поле 'balance'");
                throw new RuntimeException("Ответ не содержит поле 'balance'");
            }
        } catch (Exception e) {
            log.error("Ошибка при получении баланса FreeKassa", e);
            throw new RuntimeException("Ошибка при получении баланса FreeKassa", e);
        }
    }

    // Вспомогательные методы

    public String calculateSign(int merchantId, String amount, String currency, String orderId) {
        return md5(merchantId + ":" + amount + ":" + SECRET1 + ":" + currency + ":" + orderId);
    }

    private String calculateNotificationSign(Notification notification) {
        return md5(notification.getMERCHANT_ID() + ":" + notification.getAMOUNT() + ":" + SECRET2 + ":" + notification.getMERCHANT_ORDER_ID());
    }

    private String calculateApiSign(Map<String, Object> data) {
        StringBuilder stringToHash = new StringBuilder();

        // Добавляем поля в порядке, указанном в документации
        stringToHash.append(data.get("shopId")).append("|");
        stringToHash.append(data.get("nonce")).append("|");
        if (data.get("i") != null) {
            stringToHash.append(data.get("i")).append("|");
        }
        if (data.get("account") != null) {
            stringToHash.append(data.get("account")).append("|");
        }
        if (data.get("amount") != null) {
            stringToHash.append(data.get("amount"));
        }

        // Добавляем currency, если он присутствует
        if (data.containsKey("currency") && data.get("currency") != null) {
            stringToHash.append("|").append(data.get("currency"));
        }

        // Удаляем последний разделитель "|"
        if (stringToHash.charAt(stringToHash.length() - 1) == '|') {
            stringToHash.deleteCharAt(stringToHash.length() - 1);
        }

        log.info("Строка для подписи: {}", stringToHash);
        String sign = hash_hmac(stringToHash.toString(), API_KEY);
        log.info("Подпись: {}", sign);
        return sign;
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 algorithm not found", e);
            throw new RuntimeException(e);
        }
    }

    private String hash_hmac(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            return bytesToHex(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            log.error("Error calculating HMAC", e);
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public int getMerchantId() {
        return MERCHANT_ID;
    }

    public int getShopId() {
        return MERCHANT_ID;
    }
}