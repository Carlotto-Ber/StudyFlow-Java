package br.com.studyflow.factory;

import br.com.studyflow.model.Administrador;

import java.util.Map;

public class AdministradorFactory extends UsuarioFactory {
    @Override
    public Administrador criarUsuario(Map<String, Object> dados) {
        DadosComuns c = dadosComuns(dados);
        return comSenha(new Administrador(c.idInstituicao(), c.nome(), c.email(), c.telefone(), c.cpf()), dados);
    }
}
