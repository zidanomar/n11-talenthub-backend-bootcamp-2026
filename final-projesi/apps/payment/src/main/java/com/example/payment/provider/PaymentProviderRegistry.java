package com.example.payment.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PaymentProviderRegistry {

    private final Map<PaymentMethod, PaymentProvider> providers = new EnumMap<>(PaymentMethod.class);

    public PaymentProviderRegistry(List<PaymentProvider> impls) {
        impls.forEach(p -> providers.put(p.method(), p));
        log.info("Registered payment providers: {}", providers.keySet());
    }

    public PaymentProvider get(PaymentMethod method) {
        var provider = providers.get(method);
        if (provider == null) {
            throw new IllegalArgumentException("No payment provider for method: " + method);
        }
        return provider;
    }
}
