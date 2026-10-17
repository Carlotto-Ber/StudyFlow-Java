package br.com.studyflow.factory;

import br.com.studyflow.model.Professor;

import java.util.Map;

public class ProfessorFactory extends UsuarioFactory {
    @Override
    public Professor criarUsuario(Map<String, Object> dados) {
        DadosComuns c = dadosComuns(dados);
        return comSenha(new Professor(c.idInstituicao(), c.nome(), c.email(), c.telefone(), c.cpf()), dados);
    }
}
