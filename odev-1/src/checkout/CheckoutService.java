package checkout;

import domain.Product;
import domain.User;
import payment.IPaymentProcessor;
import payment.PaymentResult;

public class CheckoutService {
    private final User user;
    private final Product[] products;
    private final IPaymentProcessor paymentProcessor;

    public CheckoutService(User user, Product[] products, IPaymentProcessor paymentProcessor) {
        this.user = user;
        this.paymentProcessor = paymentProcessor;
        this.products = products;
    }

    public PaymentResult checkout() {
        double amount = this.total();

        if (!canAfford(amount)) {
            return PaymentResult.failure("Insufficient balance");
        }

        PaymentResult result = paymentProcessor.pay(amount);
        if (result.isSuccess()) {
            user.deduct(amount);
        }
        return result;
    }

    private double total() {
        double sum = 0;
        for (Product p : products) sum += p.getPrice();
        return sum;
    }

    private boolean canAfford(double amount) {
        return user.getBalance() >= amount;
    }
}
