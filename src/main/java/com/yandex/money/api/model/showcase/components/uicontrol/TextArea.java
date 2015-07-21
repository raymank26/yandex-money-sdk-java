package com.yandex.money.api.model.showcase.components.uicontrol;

/**
 * Text field for arbitrary user's input.
 *
 * @author Aleksandr Ershov (asershov@yamoney.com)
 */
public class TextArea extends ParameterControl {

    /**
     * Minimal text length.
     */
    public final Integer minLength;

    /**
     * Maximum text length.
     */
    public final Integer maxLength;

    protected TextArea(Builder builder) {
        super(builder);
        if (builder.minLength != null && builder.maxLength != null &&
                builder.minLength > builder.maxLength) {
            throw new IllegalArgumentException("minLength > maxLength");
        }
        minLength = builder.minLength;
        maxLength = builder.maxLength;
    }

    @Override
    public boolean isValid(String value) {
        return super.isValid(value) &&
                (value == null || value.isEmpty() ||
                        (minLength == null || value.length() >= minLength) &&
                                (maxLength == null || value.length() <= maxLength));
    }

    /**
     * {@link TextArea} builder.
     */
    public static class Builder extends ParameterControl.Builder {

        private Integer minLength;
        private Integer maxLength;

        @Override
        public TextArea create() {
            return new TextArea(this);
        }

        public Builder setMinLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }
    }
}