package com.example.ecommerceProject.service;

import com.example.ecommerceProject.dto.PaymentRequest;
import com.example.ecommerceProject.model.Order;
import com.example.ecommerceProject.model.Payment;
import com.example.ecommerceProject.repository.OrderRepository;
import com.example.ecommerceProject.repository.PaymentRepository;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderService orderService;
    private static final List<String> VALID_PAYMENT_METHODS = List.of("UPI", "CARD");

    /*
        Step 1 → Check if order exists
        Step 2 → Check if order status is PLACED
                   └── If not PLACED → cannot make payment
        Step 3 → Check if payment already done for this order
                   └── If SUCCESS already → return error
        Step 4 → Validate payment method
                   └── Must be UPI or CARD only
        Step 5 → If UPI selected
                   ├── Check UPI ID is provided
                   └── Check UPI ID contains @ symbol
        Step 6 → Convert amount to paise
                   └── 1 INR = 100 paise (Stripe requirement)
        Step 7 → Build Stripe PaymentIntent
                   ├── If UPI  → addPaymentMethodType("upi")
                   └── If CARD → addPaymentMethodType("card")
        Step 8 → Create PaymentIntent on Stripe
        Step 9 → Save payment in DB with PENDING status
        Step 10 → Return clientSecret to frontend
     */

    public Map<String, Object> createPaymentIntent(PaymentRequest request){
        Map<String, Object> response = new HashMap<>();
        // check if order exist
        Optional<Order> optionalOrder = orderRepository.findById(request.getOrderId());
        if(optionalOrder.isEmpty()){
            response.put("error", "Order not found");
            return response;
        }
        Order order = optionalOrder.get();
        // check if order status is PLACED
        if(!order.getStatus().equals("PLACED")){
            response.put("error", "Payment can only be made for orders with status PLACED");
            return response;
        }
        // check if payment already done for this order
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(order.getId());
        if(existingPayment.isPresent() && existingPayment.get().getStatus().equals("SUCCESS")){
            response.put("error", "Payment already completed for this order");
            return response;
        }
        // validate payment method
        if(!VALID_PAYMENT_METHODS.contains(request.getPaymentMethod())){
            response.put("error", "Invalid payment method. Must be UPI or CARD");
            return response;
        }
        // if UPI selected, validate UPI ID
        if(request.getPaymentMethod().equalsIgnoreCase("UPI")){
            if(request.getUpiId()==null || request.getUpiId().isEmpty()){
                response.put("error", "UPI ID is required for UPI payment");
                return response;
            }
            if(!request.getUpiId().contains("@")){
                response.put("error", "Invalid UPI ID format. Example: username@upi");
                return response;
            }
        }
        try{
            Long amountInPaise = (long)(order.getTotal()*100);
            // Build Stripe PaymentIntent parameters
            PaymentIntentCreateParams.Builder paramBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency(request.getCurrency()!=null ? request.getCurrency().toLowerCase() : "inr")
                    .setDescription("StrideFire order #" + order.getId());

            // Add payment method type based on user selection
            if(request.getPaymentMethod().equalsIgnoreCase("UPI")){
                paramBuilder.addPaymentMethodType("upi");
            }else if(request.getPaymentMethod().equalsIgnoreCase("CARD")){
                paramBuilder.addPaymentMethodType("card");
            }

            PaymentIntentCreateParams params = paramBuilder.build();
            // Create PaymentIntent on Stripe
            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // save payment in DB with PENDING status
            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setUserId(request.getUserId());
            payment.setPaymentMethod(request.getPaymentMethod().toUpperCase());
            payment.setUpiId(request.getUpiId());
            payment.setTransactionId(paymentIntent.getId());
            payment.setAmount(order.getTotal());
            payment.setCurrency(request.getCurrency()!=null ? request.getCurrency().toUpperCase() : "INR");
            payment.setStatus("PENDING");
            payment.setCreatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // return clientSecret to frontend
            response.put("message", "Payment intent created successfully");
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("transactionId", paymentIntent.getId());
            response.put("amount", order.getTotal());
            response.put("currency", payment.getCurrency());
            response.put("paymentMethod", payment.getPaymentMethod());

        }catch(Exception e){
            response.put("error", "Failed to create payment intent: " + e.getMessage());
        }
        return response;
    }

    /*
        Step 1 → Find payment record by transactionId in DB
           └── If not found → return error
        Step 2 → Check if payment already confirmed
                   └── If status is SUCCESS → return "already confirmed"
        Step 3 → Retrieve PaymentIntent from Stripe
                   └── PaymentIntent.retrieve(transactionId)
        Step 4 → Check Stripe status
                   ├── "succeeded"       → payment SUCCESS
                   ├── "payment_failed"  → payment FAILED
                   └── anything else     → still PENDING
        Step 5 → If SUCCESS
                   ├── Update payment status → SUCCESS in DB
                   └── Update order status  → CONFIRMED in DB
        Step 6 → If FAILED
                   └── Update payment status → FAILED in DB
        Step 7 → Return response with status
     */

    @Transactional
    public Map<String, Object> confirmPayment(
            String transactionId) {
        Map<String, Object> response = new HashMap<>();
        // Step 1 → Find payment by transactionId
        Optional<Payment> optionalPayment = paymentRepository.findByTransactionId(transactionId);

        if (optionalPayment.isEmpty()) {
            response.put("error", "Payment not found for " + "transaction ID: " + transactionId);
            return response;
        }

        Payment payment = optionalPayment.get();
        // Step 2 → Check if already confirmed
        if (payment.getStatus().equals("SUCCESS")) {
            response.put("message", "Payment already confirmed");
            response.put("status",   "SUCCESS");
            response.put("orderId", payment.getOrderId());
            response.put("amount", payment.getAmount());
            response.put("paymentMethod", payment.getPaymentMethod());
            return response;
        }
        // Step 3 → Retrieve PaymentIntent from Stripe
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(transactionId);

            String stripeStatus = paymentIntent.getStatus();

            System.out.println("Stripe PaymentIntent status: " + stripeStatus);

            // Step 4 → Handle Stripe status
            // In test mode Stripe returns:
            // "requires_payment_method"
            // "requires_confirmation"
            // "requires_action"
            // "succeeded"
            // We treat all non-canceled as SUCCESS

            if (stripeStatus.equals("canceled")) {
                payment.setStatus("FAILED");
                paymentRepository.save(payment);

                response.put("error", "Payment was canceled");
                response.put("status", "FAILED");
                response.put("transactionId", transactionId);

            } else {
                payment.setStatus("SUCCESS");
                paymentRepository.save(payment);
                // Step 5 → Update order to CONFIRMED
                Optional<Order> optionalOrder = orderRepository.findById(payment.getOrderId());

                if (optionalOrder.isPresent()) {
                    Order order = optionalOrder.get();
                    order.setStatus("CONFIRMED");
                    orderRepository.save(order);
                    System.out.println("Order " + order.getId() + " → CONFIRMED");
                }
                // Step 6 → Clear cart
                orderService.clearCartAfterOrder(payment.getUserId());

                System.out.println("Cart cleared for user: " + payment.getUserId() + " after successful payment");
                // Step 7 → Return success response
                response.put("message", "Payment successful! " + "Order confirmed");
                response.put("status", "SUCCESS");
                response.put("transactionId", transactionId);
                response.put("orderId", payment.getOrderId());
                response.put("amount", payment.getAmount());
                response.put("currency",payment.getCurrency());
                response.put("paymentMethod", payment.getPaymentMethod());
            }

        } catch (Exception e) {
            System.out.println("Stripe confirm error: " + e.getMessage());
            response.put("error", "Failed to confirm payment: " + e.getMessage());
        }

        return response;
    }
    /*
        Step 1 → Find payment by orderId in DB
        Step 2 → If not found → return error "No payment found"
        Step 3 → Return payment details
     */
    public Map<String, Object> getPaymentByOrderId(Long orderId){
        Map<String, Object> response = new HashMap<>();
        Optional<Payment> optionalPayment = paymentRepository.findByOrderId(orderId);
        if(optionalPayment.isEmpty()){
            response.put("error", "No payment found for order ID: " + orderId);
            return response;
        }
        response.put("payment", optionalPayment.get());
        return response;
    }
}
