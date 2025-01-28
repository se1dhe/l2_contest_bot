package dev.se1dhe.bot.payments;

import com.fasterxml.jackson.databind.JsonNode;
import dev.se1dhe.bot.BotApplication;
import dev.se1dhe.bot.service.BalanceService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownContentTypeException;
import org.springframework.web.util.UriComponentsBuilder;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Log4j2
public class PaymentsService {

    private final String API_URL_V1 = "https://pay.primepayments.io/API/v1/";
    private final String API_URL_V2 = "https://pay.primepayments.io/API/v2/";

    @Value("${primepayments.api.secret1}")
    private String SECRET1;
    @Value("${primepayments.api.secret2}")
    private String SECRET2;
    @Value("${primepayments.api.payoutKey}")
    private String PAYOUT_KEY;
    @Value("${primepayments.api.projectId}")
    private long PROJECT_ID;

    private final RestTemplate restTemplate;
    private final BalanceService balanceService;

    @Autowired
    public PaymentsService(RestTemplate restTemplate, BalanceService balanceService) {
        this.restTemplate = restTemplate;
        this.balanceService = balanceService;
    }

    //region Вспомогательные методы
    private String calculateSign(String... params) {
        StringBuilder sb = new StringBuilder();
        for (String param : params) {
            if (param != null) {
                sb.append(param);
            }
        }
        String sign = md5(sb.toString());
        log.debug("Рассчитанная подпись: {}", sign);
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
            log.error("Ошибка при вычислении MD5", e);
            throw new RuntimeException(e);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        log.debug("Заголовки запроса: {}", headers);
        return headers;
    }

    private <T> T sendPostRequest(String action, MultiValueMap<String, String> params, Class<T> responseType, String apiVersion) {
        String apiUrl = (apiVersion.equals("v1")) ? API_URL_V1 : API_URL_V2;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);
        params.add("action", action);
        HttpEntity<?> request = new HttpEntity<>(params, getHeaders());
        log.info("Отправка POST запроса на URL: {}", builder.toUriString());
        log.info("Параметры запроса: {}", params);
        String url = builder.toUriString() + "?" + params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                .reduce((s1, s2) -> s1 + "&" + s2)
                .orElse("");
        log.info("URL запроса: {}", url);
        try {
            ResponseEntity<T> response = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, request, responseType);
            log.info("Получен ответ со статусом: {}", response.getStatusCode());
            log.debug("Тело ответа: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP ошибка при выполнении запроса: {}", e.getResponseBodyAsString());
            throw new RuntimeException("Ошибка при обращении к API PrimePayments: " + e.getResponseBodyAsString(), e);
        } catch (UnknownContentTypeException e) {
            log.error("Неизвестный тип контента в ответе: {}", e.getMessage());
            String responseBody = restTemplate.exchange(builder.toUriString(), HttpMethod.POST, request, String.class).getBody();
            log.error("Тело ответа: {}", responseBody);
            throw new RuntimeException("Не удалось извлечь ответ из-за неизвестного типа контента", e);
        }
    }

    private <T> T sendGetRequest(String action, MultiValueMap<String, String> params, Class<T> responseType, String apiVersion) {
        String apiUrl = (apiVersion.equals("v1")) ? API_URL_V1 : API_URL_V2;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl);

        if (params == null) {
            params = new LinkedMultiValueMap<>();
        }
        params.add("action", action);

        if (!params.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String key = entry.getKey();
                List<String> values = entry.getValue();
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        builder.queryParam(key, value);
                    }
                }
            }
        }

        HttpEntity<?> request = new HttpEntity<>(getHeaders());
        log.info("Отправка GET запроса на URL: {}", builder.toUriString());
        log.info("Параметры запроса: {}", params);
        ResponseEntity<T> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, request, responseType);
        log.info("URL запроса: {}", builder.toUriString());
        log.info("Получен ответ со статусом: {}", response.getStatusCode());
        log.debug("Тело ответа: {}", response.getBody());
        return response.getBody();
    }


    private String formatTimestamp(long timestamp) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String formattedDate = formatter.format(Instant.ofEpochSecond(timestamp));
        log.debug("Отформатированная дата: {}", formattedDate);
        return formattedDate;
    }


    public InitPaymentResponse initPayment(InitPaymentRequest request) {
        log.info("Инициализация платежа: {}", request);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(request.getProject()));
        params.add("sum", request.getSum().toPlainString());
        params.add("currency", request.getCurrency());
        params.add("innerID", request.getInnerID());
        params.add("email", request.getEmail());

        if (request.getComment() != null) params.add("comment", request.getComment());
        if (request.getNeedFailNotice() != null)
            params.add("needFailNotice", request.getNeedFailNotice() ? "1" : "0");
        if (request.getLang() != null) params.add("lang", request.getLang());
        if (request.getPayWay() != null) params.add("payWay", String.valueOf(request.getPayWay()));
        if (request.getStrictPayWay() != null)
            params.add("strict_payWay", request.getStrictPayWay() ? "1" : "0");
        if (request.getBlockPayWay() != null) params.add("block_payWay", request.getBlockPayWay() ? "1" : "0");
        if (request.getDirectPay() != null) params.add("directPay", request.getDirectPay() ? "1" : "0");

        params.add("sign", calculateSign(SECRET1, "initPayment", String.valueOf(request.getProject()), request.getSum().toPlainString(), request.getCurrency(), request.getInnerID(), request.getEmail(),
                request.getPayWay() != null ? String.valueOf(request.getPayWay()) : null));

        return sendPostRequest("initPayment", params, InitPaymentResponse.class, "v2");

    }


    public OrderInfoResponse getOrderInfo(long orderId) {
        log.info("Получение информации о заказе с ID: {}", orderId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(PROJECT_ID));
        params.add("orderID", String.valueOf(orderId));
        params.add("sign", calculateSign(SECRET1, "getOrderInfo", String.valueOf(PROJECT_ID), String.valueOf(orderId)));
        return sendPostRequest("getOrderInfo", params, OrderInfoResponse.class, "v2");
    }


    public RefundResponse refund(long orderId) {
        log.info("Возврат средств по заказу с ID: {}", orderId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(PROJECT_ID));
        params.add("orderID", String.valueOf(orderId));
        params.add("sign", calculateSign(SECRET1, "refund", String.valueOf(PROJECT_ID), String.valueOf(orderId)));
        return sendPostRequest("refund", params, RefundResponse.class, "v2");
    }

    public InitPayoutResponse initPayout(InitPayoutRequest request) {
        log.info("Инициализация выплаты: {}", request);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(request.getProject()));
        params.add("sum", request.getSum().toPlainString());
        params.add("currency", request.getCurrency());
        params.add("payWay", String.valueOf(request.getPayWay()));
        params.add("email", request.getEmail());
        params.add("purse", request.getPurse());

        // Дополнительные параметры для карт
        if (request.getPayWay() == 1) {
            if (request.getDocNumber() != null) params.add("doc_number", request.getDocNumber());
            if (request.getCardholderFirstName() != null)
                params.add("cardholder_first_name", request.getCardholderFirstName());
            if (request.getCardholderLastName() != null)
                params.add("cardholder_last_name", request.getCardholderLastName());
        }

        if (request.getComment() != null) params.add("comment", request.getComment());
        if (request.getNeedUnique() != null) params.add("needUnique", request.getNeedUnique());

        params.add("sign", calculateSign(PAYOUT_KEY, "initPayout", String.valueOf(request.getProject()), request.getSum().toPlainString(), request.getCurrency(), String.valueOf(request.getPayWay()), request.getEmail(), request.getPurse()));

        return sendPostRequest("initPayout", params, InitPayoutResponse.class, "v1");
    }


    public ProjectBalanceResponse getProjectBalance() {
        log.info("Получение баланса проекта");
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(PROJECT_ID));
        params.add("sign", calculateSign(SECRET1, "getProjectBalance", String.valueOf(PROJECT_ID)));

        return sendPostRequest("getProjectBalance", params, ProjectBalanceResponse.class, "v2");
    }


    public PayoutInfoResponse getPayoutInfo(long payoutId) {
        log.info("Получение информации о выплате с ID: {}", payoutId);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(PROJECT_ID));
        params.add("payoutID", String.valueOf(payoutId));
        params.add("sign", calculateSign(SECRET1, "getPayoutInfo", String.valueOf(PROJECT_ID), String.valueOf(payoutId)));

        return sendPostRequest("getPayoutInfo", params, PayoutInfoResponse.class, "v2");
    }


    public ProjectInfoResponse getProjectInfo() {
        log.info("Получение информации о проекте");
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("project", String.valueOf(PROJECT_ID));
        params.add("sign", calculateSign(SECRET1, "getProjectInfo", String.valueOf(PROJECT_ID)));

        return sendPostRequest("getProjectInfo", params, ProjectInfoResponse.class, "v2");
    }

    public ExchangeRatesResponse getExchangeRates() {
        log.info("Получение курсов обмена");
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("sign", calculateSign(SECRET1, "getExchangeRates"));
        return sendPostRequest("getExchangeRates", params, ExchangeRatesResponse.class, "v2");
    }
    //endregion

    //region Обработчики вебхуков


    public String handleOrderPayedNotification(OrderPayedNotification notification) {
        log.info("Получено уведомление об оплате заказа: {}", notification);
        String calculatedSign = calculateSign(SECRET2, String.valueOf(notification.getOrderID()), String.valueOf(notification.getPayWay()), notification.getInnerID(), notification.getSum().toPlainString(), notification.getWebmasterProfit().toPlainString());
        if (!Objects.equals(notification.getSign(), calculatedSign)) {
            log.error("Неверная подпись уведомления об оплате!");
            throw new RuntimeException("Invalid signature");
        }

        // Обработка уведомления об оплате
        log.info("Обработка уведомления об оплате заказа с ID: {}", notification.getOrderID());

        // Получаем innerID (в примере это ID пользователя в Telegram)
        String innerId = notification.getInnerID();
        Long telegramUserId = Long.valueOf(innerId);

        // Отправляем сообщение пользователю
        try {
            BotApplication.telegramBot.execute(SendMessage.builder()
                    .chatId(telegramUserId)
                    .text("Ваш платеж на сумму " + notification.getSum() + " " + notification.getCurrency() + " прошел успешно!")
                    .build());
            log.info("Пользователю {} отправлено уведомление об успешной оплате.", telegramUserId);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения пользователю {}", telegramUserId, e);
        }

        // Пополнение баланса пользователя
        balanceService.deposit(telegramUserId, notification.getSum());
        log.info("Баланс пользователя {} пополнен на сумму {}.", telegramUserId, notification.getSum());

        return "OK";
    }


    public String handleOrderCancelNotification(OrderCancelNotification notification) {
        log.info("Получено уведомление об отмене заказа: {}", notification);
        String calculatedSign = calculateSign(SECRET2, String.valueOf(notification.getOrderID()), notification.getInnerID());
        if (!Objects.equals(notification.getSign(), calculatedSign)) {
            log.error("Неверная подпись уведомления об отмене!");
            throw new RuntimeException("Invalid signature");
        }

        // Обработка уведомления об отмене
        log.info("Обработка уведомления об отмене заказа с ID: {}", notification.getOrderID());
        // ... (логика обработки, например, обновление статуса заказа в БД)

        return "OK";
    }
    //endregion

    public long getProjectId() {
        return PROJECT_ID;
    }
}