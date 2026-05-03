package com.example.payment.provider.iyzico;

import com.example.payment.dto.InitiatePaymentRequest;
import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.dto.VerifyPaymentResult;
import com.example.payment.provider.PaymentMethod;
import com.example.payment.provider.PaymentProvider;
import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.Cancel;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import com.iyzipay.model.Currency;
import com.iyzipay.model.Locale;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.model.Status;
import com.iyzipay.request.CreateCancelRequest;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IyzicoPaymentProvider implements PaymentProvider {

    private final Options options;

    @Value("${payment.callback-base-url:http://localhost:8080/api/payments/callback}")
    private String callbackBaseUrl;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.IYZICO;
    }

    @Override
    public InitiatePaymentResponse initiate(InitiatePaymentRequest request) {
        var req = new CreateCheckoutFormInitializeRequest();
        req.setLocale(Locale.EN.getValue());
        req.setConversationId(String.valueOf(request.orderId()));
        req.setPrice(request.totalPrice().setScale(2, java.math.RoundingMode.HALF_UP));
        req.setPaidPrice(request.totalPrice().setScale(2, java.math.RoundingMode.HALF_UP));
        req.setCurrency(Currency.TRY.name());
        req.setBasketId("B" + request.orderId());
        req.setPaymentGroup(PaymentGroup.PRODUCT.name());
        req.setCallbackUrl(callbackBaseUrl + "/" + method().name().toLowerCase()
                + "?orderId=" + request.orderId());
        req.setEnabledInstallments(List.of(2, 3, 6, 9));
        req.setDebitCardAllowed(Boolean.TRUE);
        req.setForceThreeDS(Boolean.TRUE.equals(request.forceThreeDS()) ? 1 : 0);
        req.setPaymentWithNewCardEnabled(request.paymentWithNewCardEnabled() == null
                ? Boolean.TRUE
                : request.paymentWithNewCardEnabled());
        if (request.cardUserKey() != null && !request.cardUserKey().isBlank()) {
            req.setCardUserKey(request.cardUserKey());
        }

        var b = request.buyer();
        Buyer buyer = new Buyer();
        buyer.setId(b.id());
        buyer.setName(b.name());
        buyer.setSurname(b.surname());
        buyer.setGsmNumber(b.phone());
        buyer.setEmail(b.email());
        buyer.setIdentityNumber(b.identityNumber());
        buyer.setRegistrationAddress(b.addressLine());
        buyer.setCity(b.city());
        buyer.setCountry(b.country());
        buyer.setZipCode(b.zipCode());
        buyer.setIp(b.ip() != null ? b.ip() : "127.0.0.1");
        req.setBuyer(buyer);

        Address address = new Address();
        address.setContactName(b.name() + " " + b.surname());
        address.setCity(b.city());
        address.setCountry(b.country());
        address.setAddress(b.addressLine());
        address.setZipCode(b.zipCode());
        req.setShippingAddress(address);
        req.setBillingAddress(address);

        List<com.iyzipay.model.BasketItem> basketItems = new ArrayList<>();
        for (var i : request.items()) {
            com.iyzipay.model.BasketItem bi = new com.iyzipay.model.BasketItem();
            bi.setId(i.id());
            bi.setName(i.name());
            bi.setCategory1(i.category() != null ? i.category() : "general");
            bi.setItemType(BasketItemType.PHYSICAL.name());
            bi.setPrice(i.price().setScale(2, java.math.RoundingMode.HALF_UP));
            basketItems.add(bi);
        }
        normalizeBasketTotals(basketItems, request.totalPrice());
        req.setBasketItems(basketItems);

        CheckoutFormInitialize result = CheckoutFormInitialize.create(req, options);

        if (!Status.SUCCESS.getValue().equals(result.getStatus())) {
            log.error("Iyzipay initiate failed orderId={} status={} errorCode={} errorMessage={}",
                    request.orderId(), result.getStatus(), result.getErrorCode(), result.getErrorMessage());
            throw new IllegalStateException("Iyzipay initiate failed: " + result.getErrorMessage());
        }
        if (result.getToken() == null || result.getToken().isBlank()
                || result.getPaymentPageUrl() == null || result.getPaymentPageUrl().isBlank()) {
            log.error("Iyzipay initiate returned incomplete response orderId={} status={} tokenPresent={} paymentPageUrlPresent={}",
                    request.orderId(), result.getStatus(), result.getToken() != null && !result.getToken().isBlank(),
                    result.getPaymentPageUrl() != null && !result.getPaymentPageUrl().isBlank());
            throw new IllegalStateException("Iyzipay initiate returned incomplete checkout response");
        }

        log.info("Iyzipay checkout initiated orderId={} token={} paymentPageUrl={} storedCardContext={}",
                request.orderId(), result.getToken(), result.getPaymentPageUrl(),
                request.cardUserKey() != null && !request.cardUserKey().isBlank());
        return new InitiatePaymentResponse(
                request.orderId(),
                PaymentMethod.IYZICO,
                result.getToken(),
                result.getPaymentPageUrl()
        );
    }

    @Override
    public VerifyPaymentResult verify(String token) {
        var req = new RetrieveCheckoutFormRequest();
        req.setLocale(Locale.EN.getValue());
        req.setToken(token);

        CheckoutForm form = CheckoutForm.retrieve(req, options);

        log.info("Iyzipay verify raw: status={} paymentStatus={} conversationId={} paymentId={} errorCode={} errorMessage={}",
                form.getStatus(), form.getPaymentStatus(), form.getConversationId(),
                form.getPaymentId(), form.getErrorCode(), form.getErrorMessage());

        Long orderId = parseOrderId(form.getConversationId());

        if (!Status.SUCCESS.getValue().equals(form.getStatus())) {
            log.warn("Iyzipay verify status not SUCCESS token={} status={} errorCode={} errorMessage={}",
                    token, form.getStatus(), form.getErrorCode(), form.getErrorMessage());
            return new VerifyPaymentResult(orderId, false,
                    form.getErrorMessage() != null ? form.getErrorMessage() : "Payment not completed",
                    form.getPaymentId());
        }

        if (!form.verifySignature(options.getSecretKey())) {
            log.error("Iyzipay signature verification failed — possible tampering. token={} orderId={}", token, orderId);
            return new VerifyPaymentResult(orderId, false, "Signature verification failed", null);
        }

        boolean successfulPaymentStatus = "SUCCESS".equalsIgnoreCase(form.getPaymentStatus());
        boolean hasPaymentId = form.getPaymentId() != null && !form.getPaymentId().isBlank();
        boolean accepted = successfulPaymentStatus && hasPaymentId;
        String reason = null;
        if (!accepted) {
            reason = form.getErrorMessage() != null
                    ? form.getErrorMessage()
                    : (!hasPaymentId ? "Iyzipay did not return payment id" : "Payment failed");
        }
        log.info("Iyzipay verify orderId={} accepted={} paymentStatus={} paymentId={}",
                orderId, accepted, form.getPaymentStatus(), form.getPaymentId());
        return new VerifyPaymentResult(orderId, accepted, reason, form.getPaymentId());
    }

    @Override
    public void refund(Long orderId, String providerPaymentId) {
        var req = new CreateCancelRequest();
        req.setLocale(Locale.EN.getValue());
        req.setConversationId(String.valueOf(orderId));
        req.setPaymentId(providerPaymentId);
        req.setIp("127.0.0.1");

        Cancel result = Cancel.create(req, options);

        log.info("Iyzipay cancel orderId={} paymentId={} status={} errorCode={} errorMessage={}",
                orderId, providerPaymentId, result.getStatus(), result.getErrorCode(), result.getErrorMessage());

        if (!Status.SUCCESS.getValue().equals(result.getStatus())) {
            throw new IllegalStateException("Iyzipay cancel failed: " + result.getErrorMessage());
        }
    }

    private Long parseOrderId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Iyzipay verify response did not include conversationId");
            return null;
        }
        try {
            return Long.parseLong(conversationId);
        } catch (Exception e) {
            log.error("Cannot parse conversationId as orderId: {}", conversationId);
            throw new IllegalStateException("Invalid conversationId: " + conversationId);
        }
    }

    /**
     * Iyzipay requires sum(basketItems.price) == price. Adjust the last item to absorb rounding.
     */
    private void normalizeBasketTotals(List<com.iyzipay.model.BasketItem> items, BigDecimal target) {
        BigDecimal sum = items.stream()
                .map(com.iyzipay.model.BasketItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = target.setScale(2, java.math.RoundingMode.HALF_UP).subtract(sum);
        if (diff.compareTo(BigDecimal.ZERO) != 0 && !items.isEmpty()) {
            var last = items.get(items.size() - 1);
            BigDecimal adjusted = last.getPrice().add(diff)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            last.setPrice(adjusted);
        }
    }
}
