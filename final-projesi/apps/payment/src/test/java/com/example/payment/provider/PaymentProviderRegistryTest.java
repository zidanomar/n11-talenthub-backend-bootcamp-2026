package com.example.payment.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PaymentProviderRegistry")
class PaymentProviderRegistryTest {

    @Test
    @DisplayName("registered method → returns same provider instance")
    void getRegistered() {
        var provider = mock(PaymentProvider.class);
        when(provider.method()).thenReturn(PaymentMethod.IYZICO);

        var registry = new PaymentProviderRegistry(List.of(provider));

        assertThat(registry.get(PaymentMethod.IYZICO)).isSameAs(provider);
    }

    @Test
    @DisplayName("empty registry → IllegalArgumentException with method name")
    void getUnknown() {
        var registry = new PaymentProviderRegistry(List.of());

        assertThatThrownBy(() -> registry.get(PaymentMethod.IYZICO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("IYZICO");
    }

    @Test
    @DisplayName("duplicate providers for same method → last one wins (EnumMap put behavior)")
    void duplicateOverrides() {
        var first = mock(PaymentProvider.class);
        var second = mock(PaymentProvider.class);
        when(first.method()).thenReturn(PaymentMethod.IYZICO);
        when(second.method()).thenReturn(PaymentMethod.IYZICO);

        var registry = new PaymentProviderRegistry(List.of(first, second));

        assertThat(registry.get(PaymentMethod.IYZICO)).isSameAs(second);
    }

    @Test
    @DisplayName("get(null) → throws (EnumMap rejects null key)")
    void getNull() {
        var registry = new PaymentProviderRegistry(List.of());

        assertThatThrownBy(() -> registry.get(null))
                .isInstanceOfAny(NullPointerException.class, IllegalArgumentException.class);
    }
}
