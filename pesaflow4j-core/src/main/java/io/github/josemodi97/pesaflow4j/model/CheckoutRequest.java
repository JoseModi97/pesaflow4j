package io.github.josemodi97.pesaflow4j.model;

import io.github.josemodi97.pesaflow4j.exception.Pesaflow4jValidationException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single payment to build a checkout payload for: who is paying, how
 * much, what it's for, and where to send them / notify on completion.
 *
 * <p>Build one with {@link #builder()}. Only {@code amount}, {@code reference},
 * {@code description}, {@code name} and {@code idNumber} are required.
 */
public final class CheckoutRequest {

    private final BigDecimal amount;
    private final String reference;
    private final String description;
    private final String name;
    private final String idNumber;
    private final String phone;
    private final String email;
    private final String currency;
    private final String callbackUrl;
    private final String notifyUrl;
    private final Boolean sendStkPush;
    private final String pictureUrl;
    private final String format;
    private final Map<String, String> extra;

    private CheckoutRequest(Builder builder) {
        this.amount = builder.amount;
        this.reference = builder.reference;
        this.description = builder.description;
        this.name = builder.name;
        this.idNumber = builder.idNumber;
        this.phone = builder.phone;
        this.email = builder.email;
        this.currency = builder.currency;
        this.callbackUrl = builder.callbackUrl;
        this.notifyUrl = builder.notifyUrl;
        this.sendStkPush = builder.sendStkPush;
        this.pictureUrl = builder.pictureUrl;
        this.format = builder.format;
        this.extra = new LinkedHashMap<>(builder.extra);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** @throws Pesaflow4jValidationException if a required field is missing */
    public void validate() {
        requireNonBlank("amount", amount == null ? null : amount.toPlainString(),
                "amount (how much to charge, e.g. 500)");
        requireNonBlank("reference", reference, "reference (a unique ID for this payment, e.g. 'INV-0001')");
        requireNonBlank("description", description, "description (what the payment is for, e.g. 'School fees')");
        requireNonBlank("name", name, "name (the payer's full name)");
        requireNonBlank("idNumber", idNumber, "idNumber (the payer's National ID or Passport number)");
    }

    private static void requireNonBlank(String field, String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new Pesaflow4jValidationException(
                    "CheckoutRequest is missing: " + label + ". Set it via CheckoutRequest.builder()." + field + "(...).");
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public Boolean getSendStkPush() {
        return sendStkPush;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getFormat() {
        return format;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    /** Builder for {@link CheckoutRequest}. */
    public static final class Builder {
        private BigDecimal amount;
        private String reference;
        private String description;
        private String name;
        private String idNumber;
        private String phone;
        private String email;
        private String currency;
        private String callbackUrl;
        private String notifyUrl;
        private Boolean sendStkPush;
        private String pictureUrl;
        private String format;
        private final Map<String, String> extra = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = BigDecimal.valueOf(amount);
            return this;
        }

        public Builder amount(String amount) {
            this.amount = (amount == null || amount.trim().isEmpty()) ? null : new BigDecimal(amount.trim());
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder idNumber(String idNumber) {
            this.idNumber = idNumber;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder callbackUrl(String callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public Builder notifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }

        public Builder sendStkPush(boolean sendStkPush) {
            this.sendStkPush = sendStkPush;
            return this;
        }

        public Builder pictureUrl(String pictureUrl) {
            this.pictureUrl = pictureUrl;
            return this;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder extra(String key, String value) {
            this.extra.put(key, value);
            return this;
        }

        public CheckoutRequest build() {
            return new CheckoutRequest(this);
        }
    }
}
