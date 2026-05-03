MVN := $(shell which mvn 2>/dev/null || find ~/.m2/wrapper/dists -name "mvn" -type f 2>/dev/null | sort -V | tail -1)
APPS_DIR := apps

.PHONY: test \
        test-cart test-product test-order test-payment \
        test-user test-shipping test-gateway \
        test-cart-verbose test-product-verbose test-order-verbose \
        test-payment-verbose test-user-verbose test-shipping-verbose test-gateway-verbose

# ── All ──────────────────────────────────────────────────────────────────────

test: test-cart test-product test-order test-payment test-user test-shipping test-gateway

# ── Per service ───────────────────────────────────────────────────────────────

test-cart:
	$(MVN) test -f $(APPS_DIR)/cart/pom.xml

test-product:
	$(MVN) test -f $(APPS_DIR)/product/pom.xml

test-order:
	$(MVN) test -f $(APPS_DIR)/order/pom.xml

test-payment:
	$(MVN) test -f $(APPS_DIR)/payment/pom.xml

test-user:
	$(MVN) test -f $(APPS_DIR)/user/pom.xml

test-shipping:
	$(MVN) test -f $(APPS_DIR)/shipping/pom.xml

test-gateway:
	$(MVN) test -f $(APPS_DIR)/gateway/pom.xml

# ── Verbose variants (full Surefire output) ──────────────────────────────────

test-cart-verbose:
	$(MVN) test -f $(APPS_DIR)/cart/pom.xml -Dsurefire.useFile=false

test-product-verbose:
	$(MVN) test -f $(APPS_DIR)/product/pom.xml -Dsurefire.useFile=false

test-order-verbose:
	$(MVN) test -f $(APPS_DIR)/order/pom.xml -Dsurefire.useFile=false

test-payment-verbose:
	$(MVN) test -f $(APPS_DIR)/payment/pom.xml -Dsurefire.useFile=false

test-user-verbose:
	$(MVN) test -f $(APPS_DIR)/user/pom.xml -Dsurefire.useFile=false

test-shipping-verbose:
	$(MVN) test -f $(APPS_DIR)/shipping/pom.xml -Dsurefire.useFile=false

test-gateway-verbose:
	$(MVN) test -f $(APPS_DIR)/gateway/pom.xml -Dsurefire.useFile=false
