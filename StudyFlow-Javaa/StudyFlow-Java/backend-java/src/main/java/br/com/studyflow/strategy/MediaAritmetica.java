package br.com.studyflow.strategy;

import java.util.List;

public class MediaAritmetica implements CalculoMedia {
    @Override
    public Double mediaDisciplina(List<NotaAvaliacao> notas) {
        return notas.isEmpty() ? null : notas.stream().mapToDouble(NotaAvaliacao::nota).average().orElse(0);
    }

    @Override
    public Double mediaGeral(List<MediaDisciplina> disciplinas) {
        return disciplinas.isEmpty() ? null : disciplinas.stream().mapToDouble(MediaDisciplina::media).average().orElse(0);
    }

    @Override
    public String rotulo(double valor) {
        return CalculoMedia.numero(valor);
    }

    @Override
    public String descricao() {
        return "Média aritmética";
    }
}
