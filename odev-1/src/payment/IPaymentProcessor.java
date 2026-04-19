package payment;

public interface IPaymentProcessor {
    String getMethod();
    PaymentResult pay(double amount);
}
