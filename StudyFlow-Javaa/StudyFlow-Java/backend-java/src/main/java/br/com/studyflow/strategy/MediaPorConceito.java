package br.com.studyflow.strategy;

import java.util.List;

/** Competências e conceito calcula a média simples e a traduz em um conceito */
public class MediaPorConceito implements CalculoMedia {
    private final MediaAritmetica base = new MediaAritmetica();

    @Override
    public Double mediaDisciplina(List<NotaAvaliacao> notas) {
        return base.mediaDisciplina(notas);
    }

    @Override
    public Double mediaGeral(List<MediaDisciplina> disciplinas) {
        return base.mediaGeral(disciplinas);
    }

    @Override
    public String rotulo(double valor) {
        if (valor >= 8.5) {
            return "Excelente";
        }
        if (valor >= 7.0) {
            return "Proficiente";
        }
        if (valor >= 5.0) {
            return "Em desenvolvimento";
        }
        return "Não atingiu";
    }

    @Override
    public String descricao() {
        return "Competências e conceito";
    }
}
