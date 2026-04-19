package app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Supplier;
import checkout.CheckoutService;
import domain.Product;
import domain.User;
import payment.*;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        Map<Integer, PaymentOption> processors = new LinkedHashMap<>();
        processors.put(1, new PaymentOption("Credit Card", CreditCard::new));
        processors.put(2, new PaymentOption("PayPal", () -> {
            System.out.print("PayPal email: ");
            return new PayPal(readLine());
        }));

        User user = new User();

        Map<Integer, Product> catalog = new LinkedHashMap<>();
        for (Product p : new Product[] {
                new Product(1, "key chain", 1000),
                new Product(2, "mug", 1000),
                new Product(3, "golden mug", 9000)
        }) {
            catalog.put(p.getId(), p);
        }

        Map<Product, Integer> cart = new LinkedHashMap<>();

        System.out.println("=== Welcome ===");
        System.out.println("Balance: " + user.getBalance());

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1 -> listProducts(catalog);
                case 2 -> addToCart(catalog, cart);
                case 3 -> removeFromCart(cart);
                case 4 -> viewCart(cart);
                case 5 -> {
                    if (checkout(user, cart, processors)) running = false;
                }

                case 0 -> running = false;
                default -> System.out.println("Invalid option. Try 0-5.");
            }
        }

        System.out.println("=== Bye ===");
        System.out.println("Final balance: " + user.getBalance());
        scanner.close();
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("-- Menu --");
        System.out.println("1. List products");
        System.out.println("2. Add to cart");
        System.out.println("3. Remove from cart");
        System.out.println("4. View cart");
        System.out.println("5. Checkout");
        System.out.println("0. Quit");
    }

    private static void listProducts(Map<Integer, Product> catalog) {
        System.out.println("-- Products --");
        for (Product p : catalog.values()) {
            System.out.println(p.getId() + ". " + p.getName() + " - " + p.getPrice());
        }
    }

    private static void addToCart(Map<Integer, Product> catalog, Map<Product, Integer> cart) {
        listProducts(catalog);
        int id = readInt("Product id (0 to cancel): ");
        if (id == 0) return;
        Product product = catalog.get(id);
        if (product == null) {
            System.out.println("No product with id " + id + ".");
            return;
        }
        int qty = readInt("Quantity: ");
        if (qty <= 0) {
            System.out.println("Quantity must be positive.");
            return;
        }
        cart.merge(product, qty, Integer::sum);
        System.out.println(product.getName() + " +" + qty + " (total " + cart.get(product) + ")");
    }

    private static void removeFromCart(Map<Product, Integer> cart) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty.");
            return;
        }
        viewCart(cart);
        int id = readInt("Product id to remove (0 to cancel): ");
        if (id == 0) return;
        Product target = null;
        for (Product p : cart.keySet()) {
            if (p.getId() == id) { target = p; break; }
        }
        if (target == null) {
            System.out.println("Not in cart.");
            return;
        }
        cart.remove(target);
        System.out.println(target.getName() + " removed.");
    }

    private static void viewCart(Map<Product, Integer> cart) {
        System.out.println("-- Cart --");
        if (cart.isEmpty()) {
            System.out.println("(empty)");
            return;
        }
        double total = 0;
        for (Map.Entry<Product, Integer> e : cart.entrySet()) {
            Product p = e.getKey();
            int qty = e.getValue();
            double line = p.getPrice() * qty;
            total += line;
            System.out.println(p.getId() + ". " + p.getName() + " x" + qty + " = " + line);
        }
        System.out.println("Total: " + total);
    }

    private static boolean checkout(User user, Map<Product, Integer> cart, Map<Integer, PaymentOption> processors) {
        if (cart.isEmpty()) {
            System.out.println("Cart is empty. Add items first.");
            return false;
        }
        viewCart(cart);

        System.out.println("-- Payment --");
        for (Map.Entry<Integer, PaymentOption> e : processors.entrySet()) {
            System.out.println(e.getKey() + ". " + e.getValue().label());
        }
        System.out.println("0. Cancel");

        IPaymentProcessor processor = null;
        while (processor == null) {
            int paymentChoice = readInt("Choose payment: ");
            if (paymentChoice == 0) return false;
            PaymentOption option = processors.get(paymentChoice);
            if (option == null) {
                System.out.println("Invalid or unavailable option. Try again.");
                continue;
            }
            try {
                processor = option.factory().get();
            } catch (RuntimeException e) {
                System.out.println("Could not init payment method: " + e.getMessage());
            }
        }

        List<Product> flat = new ArrayList<>();
        for (Map.Entry<Product, Integer> e : cart.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) flat.add(e.getKey());
        }

        try {
            CheckoutService service = new CheckoutService(user, flat.toArray(new Product[0]), processor);
            PaymentResult result = service.checkout();
            if (result.isSuccess()) {
                System.out.println("Payment successful! Txn: " + result.getTransactionId());
                cart.clear();
                System.out.println("Remaining balance: " + user.getBalance());
                return true;
            }
            System.out.println("Payment failed: " + result.getMessage());
            return false;
        } catch (RuntimeException e) {
            System.out.println("Checkout error: " + e.getMessage());
            return false;
        }
    }

    private record PaymentOption(String label, Supplier<IPaymentProcessor> factory) {}

    private static String readLine() {
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println("Input stream closed. Exiting.");
            System.exit(0);
            return "";
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    System.out.println("Empty input. Enter a number.");
                    continue;
                }
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Not a number. Try again.");
            } catch (NoSuchElementException | IllegalStateException e) {
                System.out.println("Input stream closed. Exiting.");
                System.exit(0);
            }
        }
    }
}
