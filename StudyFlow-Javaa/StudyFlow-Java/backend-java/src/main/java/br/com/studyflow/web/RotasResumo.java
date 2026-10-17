package br.com.studyflow.web;

import br.com.studyflow.erros.ValidacaoException;
import br.com.studyflow.model.Calendario;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.model.Painel;
import br.com.studyflow.service.DesempenhoService;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Telas que juntam dados de várias tabelas painel, calendário e desempenho */
public class RotasResumo implements Rotas {
    @Override
    public void registrar(Roteador r) {
        r.get("/painel", c -> Resposta.ok(
                Painel.resumo(Instituicao.buscar(c.numero("instituicao")), c.numeroOpcional("estudante"))));

        r.get("/calendario", c -> {
            YearMonth mes;
            try {
                String texto = c.texto("mes", null);
                mes = texto == null ? YearMonth.now() : YearMonth.parse(texto);
            } catch (DateTimeParseException e) {
                throw new ValidacaoException("Mês inválido. Use o formato AAAA-MM.");
            }
            return Resposta.ok(Calendario.doMes(c.numero("instituicao"), c.numeroOpcional("estudante"), mes));
        });

        r.get("/desempenho", c -> {
            Instituicao instituicao = Instituicao.buscar(c.numero("instituicao"));
            instituicao.exigirModulo("desempenho");
            return Resposta.ok(DesempenhoService.calcular(instituicao, c.numeroOpcional("estudante")));
        });
    }
}
