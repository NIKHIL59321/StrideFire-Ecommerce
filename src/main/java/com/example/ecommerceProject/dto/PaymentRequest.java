package com.example.ecommerceProject.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private Long userId;
    private String paymentMethod;   // "UPI" or "CARD"
    private String upiId;           // Only for UPI (e.g. user@upi)
    private String currency;
    // "INR"

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
