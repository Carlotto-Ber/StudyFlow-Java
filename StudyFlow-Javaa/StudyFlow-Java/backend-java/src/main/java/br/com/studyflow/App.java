package br.com.studyflow;

import br.com.studyflow.core.Config;
import br.com.studyflow.web.Servidor;

import java.nio.file.Path;

/** Inicia o StudyFlow: API em /api e front-end da pasta Trabalho. */
public final class App {
    private App() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$s %3$s: %5$s%6$s%n");
        int porta = Config.porta();
        Path front = Config.pastaFront();
        new Servidor(porta, front).iniciar();
        System.out.println("StudyFlow rodando em http://localhost:" + porta);
        System.out.println("Front-end servido de: " + front);
    }
}
