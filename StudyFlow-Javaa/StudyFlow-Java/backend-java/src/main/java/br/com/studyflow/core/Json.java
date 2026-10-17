package br.com.studyflow.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {

    private final String texto;
    private int pos;

    private Json(String texto) {
        this.texto = texto;
    }

    // ------------------------------------------------------------ leitura

    public static Object parse(String texto) {
        Json p = new Json(texto);
        p.espacos();
        Object valor = p.valor();
        p.espacos();
        if (p.pos != texto.length()) {
            throw p.erro();
        }
        return valor;
    }

    private IllegalArgumentException erro() {
        return new IllegalArgumentException("JSON inválido na posição " + pos);
    }

    private void espacos() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }

    private char atual() {
        if (pos >= texto.length()) {
            throw erro();
        }
        return texto.charAt(pos);
    }

    private void esperar(char c) {
        if (atual() != c) {
            throw erro();
        }
        pos++;
    }

    private Object valor() {
        switch (atual()) {
            case '{':
                return objeto();
            case '[':
                return lista();
            case '"':
                return string();
            case 't':
                literal("true");
                return Boolean.TRUE;
            case 'f':
                literal("false");
                return Boolean.FALSE;
            case 'n':
                literal("null");
                return null;
            default:
                return numero();
        }
    }

    private void literal(String esperado) {
        if (!texto.startsWith(esperado, pos)) {
            throw erro();
        }
        pos += esperado.length();
    }

    private Map<String, Object> objeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        esperar('{');
        espacos();
        if (atual() == '}') {
            pos++;
            return mapa;
        }
        while (true) {
            espacos();
            String chave = string();
            espacos();
            esperar(':');
            espacos();
            mapa.put(chave, valor());
            espacos();
            char c = atual();
            pos++;
            if (c == '}') {
                return mapa;
            }
            if (c != ',') {
                throw erro();
            }
        }
    }

    private List<Object> lista() {
        List<Object> itens = new ArrayList<>();
        esperar('[');
        espacos();
        if (atual() == ']') {
            pos++;
            return itens;
        }
        while (true) {
            espacos();
            itens.add(valor());
            espacos();
            char c = atual();
            pos++;
            if (c == ']') {
                return itens;
            }
            if (c != ',') {
                throw erro();
            }
        }
    }

    private String string() {
        esperar('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = atual();
            pos++;
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = atual();
            pos++;
            switch (e) {
                case '"', '\\', '/' -> sb.append(e);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > texto.length()) {
                        throw erro();
                    }
                    try {
                        sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw erro();
                    }
                    pos += 4;
                }
                default -> throw erro();
            }
        }
    }

    private Object numero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) {
            pos++;
        }
        String trecho = texto.substring(inicio, pos);
        if (trecho.isEmpty()) {
            throw erro();
        }
        try {
            if (trecho.contains(".") || trecho.contains("e") || trecho.contains("E")) {
                return Double.parseDouble(trecho);
            }
            return Long.parseLong(trecho);
        } catch (NumberFormatException e) {
            throw erro();
        }
    }

    // ------------------------------------------------------------ escrita
    public static String stringify(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(sb, valor);
        return sb.toString();
    }

    private static void escrever(StringBuilder sb, Object valor) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String s) {
            escreverString(sb, s);
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escreverString(sb, String.valueOf(entrada.getKey()));
                sb.append(':');
                escrever(sb, entrada.getValue());
            }
            sb.append('}');
        } else if (valor instanceof Iterable<?> itens) {
            sb.append('[');
            boolean primeiro = true;
            for (Object item : itens) {
                if (!primeiro) {
                    sb.append(',');
                }
                primeiro = false;
                escrever(sb, item);
            }
            sb.append(']');
        } else {
            escreverString(sb, String.valueOf(Util.paraTexto(valor)));
        }
    }

    private static void escreverString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
