package br.com.studyflow.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {
    private Config() {
    }

    public static String valor(String chave, String padrao) {
        String v = System.getProperty(chave);
        if (v == null || v.isBlank()) {
            v = System.getenv(chave);
        }
        return (v == null || v.isBlank()) ? padrao : v;
    }

    public static String urlJdbc() {
        String url = valor("STUDYFLOW_DB_URL", null);
        if (url != null) {
            return url;
        }
        return "jdbc:mysql://" + valor("STUDYFLOW_DB_HOST", "localhost") + ":" + valor("STUDYFLOW_DB_PORT", "3306")
                + "/" + valor("STUDYFLOW_DB_NAME", "studyflow")
                + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true";
    }

    public static String usuario() {
        return valor("STUDYFLOW_DB_USER", "root");
    }

    public static String senha() {
        return valor("STUDYFLOW_DB_PASSWORD", "");
    }

    public static int porta() {
        return Integer.parseInt(valor("STUDYFLOW_PORT", "5000"));
    }

    public static Path pastaFront() {
        String configurada = valor("STUDYFLOW_FRONT", null);
        if (configurada != null) {
            return Paths.get(configurada).toAbsolutePath().normalize();
        }
        for (String candidato : new String[] {"../Trabalho", "Trabalho"}) {
            Path caminho = Paths.get(candidato).toAbsolutePath().normalize();
            if (Files.isDirectory(caminho)) {
                return caminho;
            }
        }
        return Paths.get("../Trabalho").toAbsolutePath().normalize();
    }
}
