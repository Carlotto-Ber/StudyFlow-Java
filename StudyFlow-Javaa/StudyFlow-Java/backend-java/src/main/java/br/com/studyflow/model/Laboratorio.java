package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;

import java.util.List;
import java.util.Map;

/** Laboratório do módulo exclusivo "laboratorio" (Instituto Atlas). */
public final class Laboratorio {
    private Laboratorio() {
    }

    public static List<Map<String, Object>> listar(long idInstituicao) {
        return DatabaseConnection.getInstance().consultar("""
                SELECT id_laboratorio AS id, nome, equipamento, estado
                  FROM laboratorios WHERE id_instituicao = ? ORDER BY nome""", idInstituicao);
    }

    public static Map<String, Object> buscar(long idLaboratorio) {
        Map<String, Object> l = DatabaseConnection.getInstance()
                .consultarUm("SELECT * FROM laboratorios WHERE id_laboratorio = ?", idLaboratorio);
        if (l == null) {
            throw new NaoEncontradoException("Laboratório não encontrado.");
        }
        return l;
    }

    public static void alterarEstado(long idLaboratorio, Object estado) {
        Map<String, Object> lab = buscar(idLaboratorio);
        Instituicao.buscar(Util.longValor(lab.get("id_instituicao"))).exigirModulo("laboratorio");
        String novo = String.valueOf(estado);
        if (!novo.equals("disponivel") && !novo.equals("manutencao")) {
            throw new ValidacaoException("Estado inválido. Use disponivel ou manutencao.");
        }
        DatabaseConnection.getInstance().executar(
                "UPDATE laboratorios SET estado = ? WHERE id_laboratorio = ?", novo, idLaboratorio);
    }
}
