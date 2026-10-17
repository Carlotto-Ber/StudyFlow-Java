package br.com.studyflow.factory;

import br.com.studyflow.core.Validadores;
import br.com.studyflow.model.Estudante;

import java.util.Map;

public class EstudanteFactory extends UsuarioFactory {
    @Override
    public Estudante criarUsuario(Map<String, Object> dados) {
        String matricula = Validadores.exigirTexto(dados.get("matricula"), "matrícula", 50, true);
        DadosComuns c = dadosComuns(dados);
        return comSenha(new Estudante(c.idInstituicao(), c.nome(), c.email(), c.telefone(), c.cpf(), matricula),
                dados);
    }
}
