package br.com.studyflow.core;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Util {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Util() {
    }

    public static Object paraTexto(Object valor) {
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime().format(DATA_HORA);
        }
        if (valor instanceof LocalDateTime t) {
            return t.format(DATA_HORA);
        }
        if (valor instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (valor instanceof LocalDate d) {
            return d.toString();
        }
        if (valor instanceof java.sql.Time h) {
            return h.toLocalTime().format(HORA);
        }
        if (valor instanceof LocalTime h) {
            return h.format(HORA);
        }
        return valor;
    }

    public static Map<String, Object> serializar(Map<String, Object> registro) {
        Map<String, Object> saida = new LinkedHashMap<>();
        registro.forEach((chave, valor) -> saida.put(chave, paraTexto(valor)));
        return saida;
    }

    public static long longValor(Object valor) {
        return ((Number) valor).longValue();
    }

    public static Long longOuNulo(Object valor) {
        return valor == null ? null : longValor(valor);
    }

    public static double doubleValor(Object valor) {
        return ((Number) valor).doubleValue();
    }

    public static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
