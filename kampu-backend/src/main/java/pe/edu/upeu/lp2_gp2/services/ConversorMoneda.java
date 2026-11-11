package pe.edu.upeu.lp2_gp2.services;

import org.springframework.stereotype.Component;
import pe.edu.upeu.lp2_gp2.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ConversorMoneda {
    public static final BigDecimal TOLERANCIA = new BigDecimal("0.005");

    private static final BigDecimal TASA_USD = new BigDecimal("3.75");
    private static final BigDecimal TASA_EUR = new BigDecimal("4.05");

    // Soles por unidad de la moneda: se guarda en PAGOS.tipo_cambio
    public BigDecimal tasa(String moneda) {
        BigDecimal tasa;
        if ("PEN".equals(moneda)) {
            tasa = BigDecimal.ONE;
        } else if ("USD".equals(moneda)) {
            tasa = TASA_USD;
        } else if ("EUR".equals(moneda)) {
            tasa = TASA_EUR;
        } else {
            throw new BusinessException("Moneda no soportada: " + moneda);
        }
        return tasa;
    }

    public BigDecimal aPEN(BigDecimal monto, String moneda) {
        return monto.multiply(tasa(moneda)).setScale(2, RoundingMode.HALF_UP);
    }
}
