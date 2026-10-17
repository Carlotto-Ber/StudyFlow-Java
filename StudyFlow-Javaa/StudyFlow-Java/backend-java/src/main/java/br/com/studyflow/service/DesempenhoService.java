package br.com.studyflow.service;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.model.Instituicao;
import br.com.studyflow.strategy.CalculoMedia;
import br.com.studyflow.strategy.CalculoMedia.MediaDisciplina;
import br.com.studyflow.strategy.CalculoMedia.NotaAvaliacao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula o desempenho por disciplina usando a estratégia de média da instituição.
 * Com idEstudante resultado do estudante Sem ele é a média da turma.
 */
public final class DesempenhoService {
    private DesempenhoService() {
    }

    public static Map<String, Object> calcular(Instituicao instituicao, Long idEstudante) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        CalculoMedia calculo = instituicao.calculoMedia();
        long idInstituicao = instituicao.getId();

        List<Map<String, Object>> disciplinas = idEstudante == null
                ? db.consultar("SELECT id_disciplina, nome, creditos FROM disciplinas WHERE id_instituicao = ? ORDER BY nome",
                        idInstituicao)
                : db.consultar("""
                        SELECT d.id_disciplina, d.nome, d.creditos
                          FROM disciplinas d JOIN matriculas m ON m.id_disciplina = d.id_disciplina
                         WHERE d.id_instituicao = ? AND m.id_estudante = ? ORDER BY d.nome""", idInstituicao, idEstudante);

        String notasSql = """
                SELECT p.id_disciplina, n.id_estudante, p.peso, n.nota
                  FROM notas n
                  JOIN provas p ON p.id_prova = n.id_prova
                  JOIN disciplinas d ON d.id_disciplina = p.id_disciplina
                 WHERE d.id_instituicao = ?""";
        List<Map<String, Object>> notas = idEstudante == null
                ? db.consultar(notasSql, idInstituicao)
                : db.consultar(notasSql + " AND n.id_estudante = ?", idInstituicao, idEstudante);

        // disciplina -> estudante -> notas
        Map<Long, Map<Long, List<NotaAvaliacao>>> agrupadas = new LinkedHashMap<>();
        Map<Long, Integer> quantidade = new LinkedHashMap<>();
        for (Map<String, Object> l : notas) {
            long disciplina = Util.longValor(l.get("id_disciplina"));
            agrupadas.computeIfAbsent(disciplina, k -> new LinkedHashMap<>())
                    .computeIfAbsent(Util.longValor(l.get("id_estudante")), k -> new ArrayList<>())
                    .add(new NotaAvaliacao(Util.doubleValor(l.get("nota")), Util.doubleValor(l.get("peso"))));
            quantidade.merge(disciplina, 1, Integer::sum);
        }

        List<Map<String, Object>> itens = new ArrayList<>();
        List<MediaDisciplina> medias = new ArrayList<>();
        for (Map<String, Object> d : disciplinas) {
            long id = Util.longValor(d.get("id_disciplina"));
            int creditos = (int) Util.longValor(d.get("creditos"));

            List<Double> dosEstudantes = new ArrayList<>();
            for (List<NotaAvaliacao> lista : agrupadas.getOrDefault(id, Map.of()).values()) {
                Double media = calculo.mediaDisciplina(lista);
                if (media != null) {
                    dosEstudantes.add(media);
                }
            }
            Double media = dosEstudantes.isEmpty() ? null
                    : dosEstudantes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            if (media != null) {
                medias.add(new MediaDisciplina(media, creditos));
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("nome", d.get("nome"));
            item.put("creditos", creditos);
            item.put("avaliacoes", quantidade.getOrDefault(id, 0));
            item.putAll(resultado(calculo, media));
            itens.add(item);
        }

        Map<String, Object> saida = new LinkedHashMap<>();
        saida.put("metodo", instituicao.getMetodoMedia());
        saida.put("descricao", calculo.descricao());
        saida.put("disciplinas", itens);
        saida.put("geral", resultado(calculo, calculo.mediaGeral(medias)));
        return saida;
    }

    private static Map<String, Object> resultado(CalculoMedia calculo, Double media) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("media", media);
        r.put("rotulo", media == null ? "Sem notas" : calculo.rotulo(media));
        r.put("percentual", media == null ? 0 : Math.round(media * 100) / 10.0);
        return r;
    }
}
