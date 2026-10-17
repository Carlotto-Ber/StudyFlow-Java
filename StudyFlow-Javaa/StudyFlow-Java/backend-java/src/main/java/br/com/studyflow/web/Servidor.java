package br.com.studyflow.web;

import br.com.studyflow.core.Json;
import br.com.studyflow.erros.ErroDeNegocio;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Servidor HTTP do JDK: entrega a API em /api e o front-end estático da pasta Trabalho */
public class Servidor {
    private static final Logger LOG = Logger.getLogger("studyflow.web");
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "ico", "image/x-icon");

    private final HttpServer servidor;
    private final Path front;
    private final Api api = new Api();

    public Servidor(int porta, Path front) throws IOException {
        this.front = front.toAbsolutePath().normalize();
        this.servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        this.servidor.createContext("/", this::tratar);
        this.servidor.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void iniciar() {
        servidor.start();
    }

    public void parar() {
        servidor.stop(0);
    }

    private void tratar(HttpExchange troca) throws IOException {
        try {
            String caminho = troca.getRequestURI().getPath();
            if (caminho.startsWith("/api/")) {
                Resposta resposta = api.tratar(troca.getRequestMethod(), caminho, consulta(troca),
                        () -> lerCorpo(troca));
                enviar(troca, resposta.status(), resposta.corpo());
            } else if (caminho.equals("/")) {
                troca.getResponseHeaders().set("Location", "/HTML/dashboard.html");
                troca.sendResponseHeaders(302, -1);
            } else if (caminho.equals("/favicon.ico")) {
                troca.sendResponseHeaders(204, -1);
            } else {
                arquivoEstatico(troca, caminho);
            }
        } catch (ErroDeNegocio e) {
            enviar(troca, e.status(), Map.of("erro", e.getMessage()));
        } catch (Throwable e) {
            LOG.log(Level.SEVERE, "Erro inesperado", e);
            enviar(troca, 500, Map.of("erro", "Erro interno. Verifique a conexão com o MySQL."));
        } finally {
            troca.close();
        }
    }

    private void arquivoEstatico(HttpExchange troca, String caminho) throws IOException {
        Path arquivo = front.resolve(caminho.substring(1)).normalize();
        if (!arquivo.startsWith(front) || !Files.isRegularFile(arquivo)) {
            enviar(troca, 404, Map.of("erro", "Recurso não encontrado."));
            return;
        }
        String nome = arquivo.getFileName().toString();
        String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1).toLowerCase() : "";
        byte[] bytes = Files.readAllBytes(arquivo);
        troca.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(extensao, "application/octet-stream"));
        troca.getResponseHeaders().set("Cache-Control", "no-cache");
        troca.sendResponseHeaders(200, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private static void enviar(HttpExchange troca, int status, Object corpo) throws IOException {
        if (corpo == null) {
            troca.sendResponseHeaders(status, -1);
            return;
        }
        byte[] bytes = Json.stringify(corpo).getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    private static String lerCorpo(HttpExchange troca) {
        try {
            return new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static Map<String, String> consulta(HttpExchange troca) {
        Map<String, String> parametros = new LinkedHashMap<>();
        String bruta = troca.getRequestURI().getRawQuery();
        if (bruta == null || bruta.isEmpty()) {
            return parametros;
        }
        for (String par : bruta.split("&")) {
            int igual = par.indexOf('=');
            String chave = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            parametros.put(URLDecoder.decode(chave, StandardCharsets.UTF_8),
                    URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
        return parametros;
    }
}
