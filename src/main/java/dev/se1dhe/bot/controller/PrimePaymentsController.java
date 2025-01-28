package dev.se1dhe.bot.controller;

import dev.se1dhe.bot.payments.OrderCancelNotification;
import dev.se1dhe.bot.payments.OrderPayedNotification;
import dev.se1dhe.bot.payments.PaymentsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PrimePaymentsController {

    private final PaymentsService paymentsService;

    public PrimePaymentsController(PaymentsService paymentsService) {
        this.paymentsService = paymentsService;
    }


    @PostMapping("/webhook/order-payed")
    public String handleOrderPayed(@RequestBody OrderPayedNotification notification) {
        return paymentsService.handleOrderPayedNotification(notification);
    }

    @PostMapping("/webhook/order-cancel")
    public String handleOrderCancelled(@RequestBody OrderCancelNotification notification) {
        return paymentsService.handleOrderCancelNotification(notification);
    }
}