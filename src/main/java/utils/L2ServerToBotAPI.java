package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class L2ServerToBotAPI {

    private static final String BOT_API_URL = "https://98ae-185-151-84-42.ngrok-free.app/api/v1/balance/adjust";
    private static final String API_KEY = "d9a8f7b2-3c5e-4e6d-a1c7-9f0e2b6d4c3a";

    public static void adjustUserBalance(long telegramId, BigDecimal amount, String reason) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("telegramId", telegramId);
        requestBody.put("amount", amount.toPlainString());
        if (reason != null && !reason.isEmpty()) {
          requestBody.put("reason", reason);
        }
        
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);


            HttpPost postRequest = new HttpPost(BOT_API_URL);
            postRequest.addHeader("Content-Type", "application/json");
            postRequest.addHeader("X-API-Key", API_KEY);
            postRequest.setEntity(new StringEntity(jsonBody, "UTF-8"));



            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(postRequest)) {

                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Response Status Code: " + statusCode);
                System.out.println("Response Body: " + responseBody);


                if (statusCode == 200) {
                    // Success!
                    System.out.println("Balance adjusted successfully.");
                } else {
                    // Handle errors
                    System.err.println("Error adjusting balance. Status code: " + statusCode);
                     System.err.println("Response body: "+ responseBody);
                }
            }

        } catch (IOException e) {
            System.err.println("Exception during API call: " + e.getMessage());
             e.printStackTrace();
        }
    }

}