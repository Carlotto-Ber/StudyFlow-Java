package br.com.studyflow.core;

import br.com.studyflow.erros.ValidacaoException;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;

public final class Validadores {
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern COR = Pattern.compile("#[0-9a-fA-F]{6}");

    private Validadores() {
    }

    public static String somenteDigitos(Object valor) {
        return valor == null ? "" : valor.toString().replaceAll("\\D", "");
    }

    public static String exigirTexto(Object valor, String campo, int maximo, boolean obrigatorio) {
        String texto = valor == null ? "" : valor.toString().strip();
        if (texto.isEmpty()) {
            if (obrigatorio) {
                throw new ValidacaoException("O campo " + campo + " é obrigatório.");
            }
            return null;
        }
        if (texto.length() > maximo) {
            throw new ValidacaoException("O campo " + campo + " aceita no máximo " + maximo + " caracteres.");
        }
        return texto;
    }

    public static long converterInteiro(Object valor, String campo) {
        if (valor instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(valor).strip());
        } catch (NumberFormatException e) {
            throw new ValidacaoException("O campo " + campo + " é obrigatório.");
        }
    }

    public static LocalDate validarData(Object valor, String campo) {
        if (valor instanceof LocalDate d) {
            return d;
        }
        if (valor instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (valor instanceof LocalDateTime t) {
            return t.toLocalDate();
        }
        try {
            String texto = String.valueOf(valor);
            return LocalDate.parse(texto.substring(0, Math.min(10, texto.length())));
        } catch (DateTimeParseException e) {
            throw new ValidacaoException("O campo " + campo + " é inválido. Use o formato AAAA-MM-DD.");
        }
    }

    public static LocalDateTime validarDataHora(Object valor, String campo) {
        if (valor instanceof LocalDateTime t) {
            return t;
        }
        if (valor instanceof Timestamp t) {
            return t.toLocalDateTime();
        }
        try {
            return LocalDateTime.parse(String.valueOf(valor).strip().replace(' ', 'T')).truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException e) {
            throw new ValidacaoException("O campo " + campo + " é inválido. Use o formato AAAA-MM-DD HH:MM:SS.");
        }
    }

    public static LocalTime validarHora(Object valor, String campo) {
        if (valor instanceof LocalTime h) {
            return h;
        }
        if (valor instanceof java.sql.Time h) {
            return h.toLocalTime();
        }
        try {
            return LocalTime.parse(String.valueOf(valor).strip());
        } catch (DateTimeParseException e) {
            throw new ValidacaoException("O campo " + campo + " é inválido. Use o formato HH:MM.");
        }
    }

    public static double validarNumero(Object valor, String campo, double minimo, double maximo) {
        double numero;
        try {
            numero = valor instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(valor).strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidacaoException("O campo " + campo + " deve ser um número.");
        }
        if (Double.isNaN(numero) || numero < minimo || numero > maximo) {
            throw new ValidacaoException("O campo " + campo + " deve estar entre " + minimo + " e " + maximo + ".");
        }
        return numero;
    }

    public static String validarCor(Object valor) {
        String cor = String.valueOf(valor).strip();
        if (!COR.matcher(cor).matches()) {
            throw new ValidacaoException("Cor inválida. Use o formato #RRGGBB.");
        }
        return cor.toLowerCase();
    }

    public static String formatarDataBr(Object valor) {
        return validarData(valor, "data").format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static String validarEmail(Object valor) {
        String email = exigirTexto(valor, "e-mail", 150, true).toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidacaoException("E-mail inválido.");
        }
        return email;
    }

    public static String validarTelefone(Object valor) {
        String d = somenteDigitos(valor);
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        throw new ValidacaoException("Telefone inválido. Informe DDD e número.");
    }

    public static char digitoCpf(String parcial) {
        int peso = parcial.length() + 1;
        int soma = 0;
        for (int i = 0; i < parcial.length(); i++) {
            soma += (parcial.charAt(i) - '0') * (peso - i);
        }
        return (char) ('0' + (soma * 10 % 11) % 10);
    }

    public static String validarCpf(Object valor) {
        String d = somenteDigitos(valor);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            throw new ValidacaoException("CPF inválido.");
        }
        if (digitoCpf(d.substring(0, 9)) != d.charAt(9) || digitoCpf(d.substring(0, 10)) != d.charAt(10)) {
            throw new ValidacaoException("CPF inválido.");
        }
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    public static String validarSenha(Object valor) {
        if (valor == null || valor.toString().length() < 6) {
            throw new ValidacaoException("A senha deve ter pelo menos 6 caracteres.");
        }
        return valor.toString();
    }
}
