# Ödev 1 — Basit Ödeme Uygulaması

Konsol tabanlı küçük bir alışveriş ve ödeme akışı. Amaç, yeni ödeme
yöntemlerinin sisteme SOLID prensiplerine uygun şekilde eklenebildiğini
göstermektir.

## Gereksinimler

- Java 21 (JDK 21 kurulu olmalı)
- Derleme için `javac`, çalıştırmak için `java`

## Proje Yapısı

```
src/
├── app/
│   └── Main.java              # CLI giriş noktası
├── domain/
│   ├── Product.java           # Ürün modeli
│   └── User.java              # Kullanıcı ve bakiye
├── checkout/
│   └── CheckoutService.java   # Sepet toplamı, bakiye kontrolü, ödeme çağrısı
└── payment/
    ├── IPaymentProcessor.java # Ödeme arayüzü (soyutlama)
    ├── BasePayment.java       # Ortak log + şablon metot
    ├── PaymentResult.java     # Ödeme sonucu (başarılı/başarısız)
    ├── CreditCard.java        # Mevcut ödeme yöntemi
    └── PayPal.java            # Yeni eklenen ödeme yöntemi
```

Paketler arasındaki bağımlılık yönü tek yönlüdür:
`app → checkout → { domain, payment }`. Döngüsel bağımlılık yoktur.

## Nasıl Çalıştırılır

Derleme:

```bash
javac --release 21 -d out $(find src -name "*.java")
```

Çalıştırma:

```bash
java -cp out app.Main
```

## Kullanım

Uygulama başladığında basit bir menü görünür:

```
1. List products      (ürünleri listele)
2. Add to cart        (sepete ekle)
3. Remove from cart   (sepetten çıkar)
4. View cart          (sepeti göster)
5. Checkout           (ödemeye geç)
0. Quit               (çıkış)
```

Ödeme adımında kullanıcı, kayıtlı ödeme yöntemlerinden birini seçer.
Geçersiz giriş, boş alan veya hatalı sayı girildiğinde program çökmek
yerine uyarı verir ve tekrar sorar.

## SOLID İlkeleri

**Open/Closed Principle (OCP)**
Yeni bir ödeme yöntemi eklemek için mevcut sınıflardan hiçbirini
değiştirmek gerekmez. `IPaymentProcessor` arayüzünü uygulayan (ya da
`BasePayment` sınıfından türeyen) yeni bir sınıf yazmak ve `Main`
içindeki kayıt tablosuna bir satır eklemek yeterlidir.

**Single Responsibility Principle (SRP)**
Her sınıfın tek bir sorumluluğu vardır:

- `Product`, `User` — veri tutar
- `PaymentResult` — ödeme sonucunu taşır
- `CheckoutService` — sepet toplamı ve ödeme orkestrasyonu
- `BasePayment` — ortak log davranışı (şablon metot)
- `CreditCard`, `PayPal` — kendi ödeme mantığı
- `Main` — sadece CLI giriş/çıkış

**Liskov Substitution Principle (LSP)**
`CreditCard` ve `PayPal`, `IPaymentProcessor` yerine sorunsuz geçer.
`CheckoutService` hangi somut sınıfla çalıştığını bilmez.

**Interface Segregation & Dependency Inversion**
`CheckoutService`, somut ödeme sınıflarına değil, `IPaymentProcessor`
soyutlamasına bağlıdır. Arayüz küçük ve odaklıdır (`pay`, `getMethod`).

## Yeni Ödeme Yöntemi Nasıl Eklenir

Örnek: Apple Pay eklemek için,

1. `src/payment/ApplePay.java` dosyasını oluştur:

   ```java
   package payment;

   public class ApplePay extends BasePayment {
       @Override
       protected PaymentResult process(double amount) {
           return PaymentResult.success("Apple Pay onaylandı");
       }

       @Override
       public String getMethod() {
           return "Apple Pay";
       }
   }
   ```

2. `src/app/Main.java` içindeki ödeme tablosuna tek satır ekle:

   ```java
   processors.put(3, new PaymentOption("Apple Pay", ApplePay::new));
   ```

Mevcut sınıflarda başka hiçbir değişiklik gerekmez. Menü, tablodan
otomatik olarak üretilir.

## Notlar

- Kod Java 21 ile derlenir. Daha yüksek sürümlere özgü önizleme
  özellikleri (örneğin isimsiz sınıflar veya `IO.println`) kullanılmaz.
- `PaymentOption`, `Main` içinde tanımlı özel bir `record` türüdür ve
  ödeme yöntemi için etiket ile fabrika (`Supplier`) tutar. Böylece
  yöntem seçildiğinde gerekirse kullanıcıdan ek bilgi (örneğin PayPal
  e-postası) alınabilir.
- Giriş doğrulaması için `readInt` ve `readLine` yardımcıları
  `NumberFormatException` ve `NoSuchElementException` gibi hataları
  yakalar.
