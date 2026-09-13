package com.garden.modoptions;

public interface NumericOption {
    double numericValue();
    double minimum();
    double maximum();
    void setNumericValue(double value);
}
