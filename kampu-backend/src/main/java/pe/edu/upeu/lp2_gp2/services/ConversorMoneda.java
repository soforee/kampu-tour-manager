package pe.edu.upeu.lp2_gp2.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ConversorMoneda {
    public static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

    @Value("${tour.tasas.USD:3.75}")
    private BigDecimal tasaUsd;

    @Value("${tour.tasas.EUR:4.05}")
    private BigDecimal tasaEur;

    public BigDecimal aPEN(BigDecimal monto, String moneda) {
        BigDecimal tasa;
        if ("PEN".equals(moneda)) {
            tasa = BigDecimal.ONE;
        } else if ("USD".equals(moneda)) {
            tasa = tasaUsd;
        } else if ("EUR".equals(moneda)) {
            tasa = tasaEur;
        } else {
            throw new BusinessException("Moneda no soportada: " + moneda);
        }
        return monto.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
    }
}
