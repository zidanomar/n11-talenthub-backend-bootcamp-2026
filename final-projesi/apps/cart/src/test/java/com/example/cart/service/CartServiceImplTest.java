package com.example.cart.service;

import com.example.cart.client.ProductClient;
import com.example.cart.client.ProductClient.ProductResponse;
import com.example.cart.client.ProductClient.ProductsPage;
import com.example.cart.dto.CartItemRequest;
import com.example.cart.dto.CartResponse;
import com.example.cart.entity.CartItem;
import com.example.cart.repository.CartRepository;
import com.example.cart.service.impl.CartServiceImpl;
import com.example.lib.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl")
class CartServiceImplTest {

    private static final String USER = "user-1";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private static ProductResponse product(Long id, String name, String price, Integer stock) {
        return new ProductResponse(id, name, new BigDecimal(price), stock);
    }

    private static CartItem item(Long id, String unitPrice, int qty) {
        return new CartItem(id, new BigDecimal(unitPrice), qty);
    }

    // ───────────────────────── getCart ─────────────────────────

    @Nested
    @DisplayName("getCart")
    class GetCart {

        @Test
        @DisplayName("empty cart → empty response, no product call")
        void empty() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of());

            CartResponse result = cartService.getCart(USER);

            assertThat(result.userId()).isEqualTo(USER);
            assertThat(result.items()).isEmpty();
            assertThat(result.grandTotal()).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(productClient);
        }

        @Test
        @DisplayName("single item enriched with product name")
        void single() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(item(1L, "50.00", 2)));
            when(productClient.getProducts(List.of(1L)))
                    .thenReturn(new ProductsPage(List.of(product(1L, "Widget", "50.00", 10))));

            CartResponse result = cartService.getCart(USER);

            assertThat(result.items()).singleElement().satisfies(i -> {
                assertThat(i.productId()).isEqualTo(1L);
                assertThat(i.name()).isEqualTo("Widget");
                assertThat(i.unitPrice()).isEqualByComparingTo("50.00");
                assertThat(i.quantity()).isEqualTo(2);
                assertThat(i.totalPrice()).isEqualByComparingTo("100.00");
            });
            assertThat(result.grandTotal()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("multiple items → grand total sums all line totals")
        void multiple() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(
                    item(1L, "10.00", 3),
                    item(2L, "25.50", 2),
                    item(3L, "0.99", 7)));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductsPage(List.of(
                            product(1L, "A", "10.00", 5),
                            product(2L, "B", "25.50", 5),
                            product(3L, "C", "0.99", 99))));

            CartResponse result = cartService.getCart(USER);

            assertThat(result.grandTotal()).isEqualByComparingTo("87.93");
            assertThat(result.items()).hasSize(3);
        }

        @Test
        @DisplayName("product missing from batch → name=null, line preserved with stored price")
        void productMissingFromBatch() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(item(99L, "5.00", 4)));
            when(productClient.getProducts(List.of(99L)))
                    .thenReturn(new ProductsPage(List.of()));

            CartResponse result = cartService.getCart(USER);

            assertThat(result.items()).singleElement().satisfies(i -> {
                assertThat(i.productId()).isEqualTo(99L);
                assertThat(i.name()).isNull();
                assertThat(i.unitPrice()).isEqualByComparingTo("5.00");
                assertThat(i.totalPrice()).isEqualByComparingTo("20.00");
            });
            assertThat(result.grandTotal()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("uses stored unitPrice not current product price (price isolation)")
        void priceFromStorageNotProduct() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(item(1L, "100.00", 1)));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductsPage(List.of(product(1L, "A", "999.99", 5))));

            CartResponse result = cartService.getCart(USER);

            assertThat(result.items().getFirst().unitPrice()).isEqualByComparingTo("100.00");
            assertThat(result.grandTotal()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("ids passed to product client preserve cart order")
        void passesIdsInOrder() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(
                    item(7L, "1.00", 1),
                    item(3L, "1.00", 1),
                    item(11L, "1.00", 1)));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductsPage(List.of()));

            cartService.getCart(USER);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
            verify(productClient).getProducts(captor.capture());
            assertThat(captor.getValue()).containsExactly(7L, 3L, 11L);
        }

        @Test
        @DisplayName("BigDecimal precision preserved across many small items")
        void bigDecimalPrecision() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(
                    item(1L, "0.10", 3),
                    item(2L, "0.20", 3)));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductsPage(List.of()));

            CartResponse result = cartService.getCart(USER);

            assertThat(result.grandTotal()).isEqualByComparingTo("0.90");
        }
    }

    // ───────────────────────── addItem ─────────────────────────

    @Nested
    @DisplayName("addItem")
    class AddItem {

        @Test
        @DisplayName("saves CartItem with product price and requested quantity")
        void savesWithProductPrice() {
            when(productClient.getProduct(1L)).thenReturn(product(1L, "Widget", "12.50", 10));
            when(cartRepository.findByUserId(USER)).thenReturn(List.of());

            cartService.addItem(USER, new CartItemRequest(1L, 4));

            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartRepository).save(eq(USER), captor.capture());
            CartItem saved = captor.getValue();
            assertThat(saved.productId()).isEqualTo(1L);
            assertThat(saved.unitPrice()).isEqualByComparingTo("12.50");
            assertThat(saved.quantity()).isEqualTo(4);
        }

        @Test
        @DisplayName("returns refreshed cart from getCart")
        void returnsRefreshedCart() {
            when(productClient.getProduct(1L)).thenReturn(product(1L, "Widget", "10.00", 10));
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(item(1L, "10.00", 2)));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductsPage(List.of(product(1L, "Widget", "10.00", 10))));

            CartResponse result = cartService.addItem(USER, new CartItemRequest(1L, 2));

            assertThat(result.userId()).isEqualTo(USER);
            assertThat(result.items()).singleElement()
                    .satisfies(i -> assertThat(i.name()).isEqualTo("Widget"));
            assertThat(result.grandTotal()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("quantity equals stock → success (boundary)")
        void exactStockBoundary() {
            when(productClient.getProduct(1L)).thenReturn(product(1L, "A", "5.00", 3));
            when(cartRepository.findByUserId(USER)).thenReturn(List.of());

            cartService.addItem(USER, new CartItemRequest(1L, 3));

            verify(cartRepository).save(eq(USER), any(CartItem.class));
        }

        @Test
        @DisplayName("quantity exceeds stock by 1 → InsufficientStockException, repo never called")
        void overByOne() {
            when(productClient.getProduct(1L)).thenReturn(product(1L, "A", "5.00", 3));

            assertThatThrownBy(() -> cartService.addItem(USER, new CartItemRequest(1L, 4)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Product 1")
                    .hasMessageContaining("3 in stock")
                    .hasMessageContaining("requested 4");

            verify(cartRepository, never()).save(any(), any());
        }

        @Test
        @DisplayName("zero stock product → InsufficientStockException")
        void zeroStock() {
            when(productClient.getProduct(1L)).thenReturn(product(1L, "A", "5.00", 0));

            assertThatThrownBy(() -> cartService.addItem(USER, new CartItemRequest(1L, 1)))
                    .isInstanceOf(InsufficientStockException.class);

            verify(cartRepository, never()).save(any(), any());
        }

        @Test
        @DisplayName("ProductClient error propagates, no save")
        void productClientErrorPropagates() {
            when(productClient.getProduct(1L)).thenThrow(new RuntimeException("downstream fail"));

            assertThatThrownBy(() -> cartService.addItem(USER, new CartItemRequest(1L, 1)))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("downstream fail");

            verify(cartRepository, never()).save(any(), any());
        }
    }

    // ───────────────────────── removeItem ─────────────────────────

    @Nested
    @DisplayName("removeItem")
    class RemoveItem {

        @Test
        @DisplayName("delegates to repository.remove")
        void delegates() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of());

            cartService.removeItem(USER, 42L);

            verify(cartRepository).remove(USER, 42L);
        }

        @Test
        @DisplayName("returns refreshed cart after removal")
        void returnsRefreshed() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of(item(2L, "5.00", 1)));
            when(productClient.getProducts(List.of(2L)))
                    .thenReturn(new ProductsPage(List.of(product(2L, "B", "5.00", 5))));

            CartResponse result = cartService.removeItem(USER, 1L);

            assertThat(result.items()).singleElement()
                    .satisfies(i -> assertThat(i.productId()).isEqualTo(2L));
            verify(cartRepository).remove(USER, 1L);
        }

        @Test
        @DisplayName("removing nonexistent productId still calls repository (idempotent)")
        void idempotent() {
            when(cartRepository.findByUserId(USER)).thenReturn(List.of());

            cartService.removeItem(USER, 999L);

            verify(cartRepository).remove(USER, 999L);
            verifyNoInteractions(productClient);
        }
    }

    // ───────────────────────── clearCart ─────────────────────────

    @Nested
    @DisplayName("clearCart")
    class ClearCart {

        @Test
        @DisplayName("delegates to repository.clear, no product calls")
        void delegates() {
            cartService.clearCart(USER);

            verify(cartRepository).clear(USER);
            verifyNoInteractions(productClient);
        }

        @Test
        @DisplayName("repository error propagates")
        void errorPropagates() {
            org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                    .when(cartRepository).clear(USER);

            assertThatThrownBy(() -> cartService.clearCart(USER))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
