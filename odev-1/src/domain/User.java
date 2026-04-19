package domain;

public class User {
    private double balance = 10000;

    public double getBalance() { return this.balance; }
    public void deduct(double amount) { this.balance -= amount; }
}
