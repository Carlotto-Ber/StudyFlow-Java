package br.com.studyflow.strategy;

import java.util.List;

/** Instituição sem regra de média as notas aparecem como foram lançadas */
public class SemCalculo implements CalculoMedia {
    @Override
    public Double mediaDisciplina(List<NotaAvaliacao> notas) {
        return null;
    }

    @Override
    public Double mediaGeral(List<MediaDisciplina> disciplinas) {
        return null;
    }

    @Override
    public String rotulo(double valor) {
        return CalculoMedia.numero(valor);
    }

    @Override
    public String descricao() {
        return "Sem cálculo institucional";
    }
}
