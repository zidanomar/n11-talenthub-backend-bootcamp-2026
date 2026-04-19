package payment;

public class PaymentResult {
    private final boolean success;
    private final long transactionId;
    private final String message;

    private PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.transactionId = System.currentTimeMillis();
    }

    public long getTransactionId() {
        return transactionId;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    public static PaymentResult success(String message) {
        return new PaymentResult(true, message);
    }

    public static PaymentResult failure(String message) {
        return new PaymentResult(false, message);
    }
}
