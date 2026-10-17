package br.com.studyflow.model;

import br.com.studyflow.core.DatabaseConnection;
import br.com.studyflow.core.Util;
import br.com.studyflow.core.Validadores;
import br.com.studyflow.erros.ConflitoException;
import br.com.studyflow.erros.NaoEncontradoException;
import br.com.studyflow.erros.ValidacaoException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public final class ReservaLaboratorio {
    private ReservaLaboratorio() {
    }

    public static long registrar(Map<String, Object> dados) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        long idLaboratorio = Validadores.converterInteiro(dados.get("id_laboratorio"), "laboratório");
        Map<String, Object> lab = Laboratorio.buscar(idLaboratorio);
        Instituicao.buscar(Util.longValor(lab.get("id_instituicao"))).exigirModulo("laboratorio");

        LocalDate data = Validadores.validarData(dados.get("data_reserva"), "data");
        LocalTime inicio = Validadores.validarHora(dados.get("hora_inicio"), "hora de início");
        LocalTime fim = Validadores.validarHora(dados.get("hora_fim"), "hora de fim");
        String responsavel = Validadores.exigirTexto(dados.get("responsavel"), "responsável", 150, true);
        String finalidade = Validadores.exigirTexto(dados.get("finalidade"), "finalidade", 150, false);

        if (!fim.isAfter(inicio)) {
            throw new ValidacaoException("O horário de fim deve ser depois do início.");
        }
        if (!"disponivel".equals(lab.get("estado"))) {
            throw new ConflitoException("O laboratório está em manutenção.");
        }
        for (Map<String, Object> r : db.consultar(
                "SELECT hora_inicio, hora_fim FROM reservas_laboratorio WHERE id_laboratorio = ? AND data_reserva = ?",
                idLaboratorio, data)) {
            LocalTime ri = Validadores.validarHora(r.get("hora_inicio"), "hora");
            LocalTime rf = Validadores.validarHora(r.get("hora_fim"), "hora");
            if (inicio.isBefore(rf) && fim.isAfter(ri)) {
                throw new ConflitoException("Já existe uma reserva neste horário.");
            }
        }
        return db.executar("""
                INSERT INTO reservas_laboratorio (id_laboratorio, responsavel, finalidade, data_reserva, hora_inicio, hora_fim)
                VALUES (?, ?, ?, ?, ?, ?)""", idLaboratorio, responsavel, finalidade, data, inicio, fim);
    }

    public static void excluir(long idReserva) {
        DatabaseConnection db = DatabaseConnection.getInstance();
        if (db.consultarUm("SELECT 1 AS ok FROM reservas_laboratorio WHERE id_reserva = ?", idReserva) == null) {
            throw new NaoEncontradoException("Reserva não encontrada.");
        }
        db.executar("DELETE FROM reservas_laboratorio WHERE id_reserva = ?", idReserva);
    }

    public static List<Map<String, Object>> listar(long idInstituicao, LocalDate aPartirDe, int limite) {
        return DatabaseConnection.getInstance().consultar("""
                SELECT r.id_reserva AS id, l.nome AS laboratorio, r.responsavel, r.finalidade,
                       r.data_reserva, r.hora_inicio, r.hora_fim
                  FROM reservas_laboratorio r
                  JOIN laboratorios l ON l.id_laboratorio = r.id_laboratorio
                 WHERE l.id_instituicao = ? AND r.data_reserva >= ?
                 ORDER BY r.data_reserva, r.hora_inicio
                 LIMIT ?""", idInstituicao, aPartirDe, limite).stream().map(Util::serializar).toList();
    }
}
