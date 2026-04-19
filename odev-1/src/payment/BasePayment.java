package payment;

abstract class BasePayment implements IPaymentProcessor {
    @Override
    public PaymentResult pay(double amount) {
        System.out.println("Processing " + this.getMethod() + " payment of " + amount);
        PaymentResult result = process(amount);
        System.out.println("Result: " + result.getMessage());
        return result;
    }

    protected abstract PaymentResult process(double amount);
}
