package payment;

public class PayPal extends BasePayment {
    private final String email;

    public PayPal(String email) {
        this.email = email;
    }

    @Override
    protected PaymentResult process(double amount) {
        if (email == null || !email.contains("@")) {
            return PaymentResult.failure("Invalid PayPal account");
        }
        return PaymentResult.success("PayPal charged " + email);
    }

    @Override
    public String getMethod() {
        return "PayPal";
    }
}
