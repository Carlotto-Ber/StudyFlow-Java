package br.com.studyflow.strategy;

import java.util.List;

/** Média das avaliações ponderada pelo peso e média geral ponderada pelos créditos */
public class MediaPonderada implements CalculoMedia {
    @Override
    public Double mediaDisciplina(List<NotaAvaliacao> notas) {
        double pesos = notas.stream().mapToDouble(NotaAvaliacao::peso).sum();
        if (notas.isEmpty() || pesos <= 0) {
            return null;
        }
        return notas.stream().mapToDouble(n -> n.nota() * n.peso()).sum() / pesos;
    }

    @Override
    public Double mediaGeral(List<MediaDisciplina> disciplinas) {
        double creditos = disciplinas.stream().mapToDouble(MediaDisciplina::creditos).sum();
        if (disciplinas.isEmpty() || creditos <= 0) {
            return null;
        }
        return disciplinas.stream().mapToDouble(d -> d.media() * d.creditos()).sum() / creditos;
    }

    @Override
    public String rotulo(double valor) {
        return CalculoMedia.numero(valor);
    }

    @Override
    public String descricao() {
        return "Média ponderada por créditos";
    }
}
