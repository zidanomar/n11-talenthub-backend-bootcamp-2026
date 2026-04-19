package payment;

public class CreditCard extends BasePayment {

    @Override
    protected PaymentResult process(double amount) {
        return PaymentResult.success("Card approved");
    }

    @Override
    public String getMethod() {
        return "Credit Card";
    }
}
