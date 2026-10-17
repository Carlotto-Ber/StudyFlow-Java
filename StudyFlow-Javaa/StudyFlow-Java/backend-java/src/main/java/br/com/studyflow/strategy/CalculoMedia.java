package br.com.studyflow.strategy;

import java.util.List;
import java.util.Locale;

/**
 * Padrão STRATEGY: cada instituição calcula a média de um jeito.
 * O método fica gravado em instituicoes.metodo_media; o componente de tela é sempre o mesmo
 */
public interface CalculoMedia {

    record NotaAvaliacao(double nota, double peso) {
    }

    record MediaDisciplina(double media, int creditos) {
    }

    /** Média de uma disciplina a partir das notas das avaliações null se não houver notas */
    Double mediaDisciplina(List<NotaAvaliacao> notas);

    /** Média geral a partir das médias de cada disciplina null se não houver disciplinas */
    Double mediaGeral(List<MediaDisciplina> disciplinas);

    /** Texto que representa uma nota ou média  */
    String rotulo(double valor);

    String descricao();

    static String numero(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f", valor);
    }

    static CalculoMedia paraMetodo(String metodo) {
        return switch (metodo == null ? "nenhuma" : metodo) {
            case "aritmetica" -> new MediaAritmetica();
            case "ponderada" -> new MediaPonderada();
            case "conceito" -> new MediaPorConceito();
            default -> new SemCalculo();
        };
    }
}
