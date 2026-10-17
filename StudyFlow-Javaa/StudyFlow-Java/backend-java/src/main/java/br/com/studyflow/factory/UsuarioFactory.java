package br.com.studyflow.factory;

import br.com.studyflow.core.Validadores;
import br.com.studyflow.model.Usuario;

import java.util.Map;

/**
 * Padrão FACTORY METHOD #1
 * UsuarioFactory define o contrato {@code criarUsuario}; cada factory concreta decide
 * qual classe instanciar (Professor ou Estudante). A validação comum e o hash da senha
 * ficam na classe base.
 */
public abstract class UsuarioFactory {

    /** FACTORY METHOD: devolve o tipo concreto de usuário. */
    public abstract Usuario criarUsuario(Map<String, Object> dados);

    protected record DadosComuns(long idInstituicao, String nome, String email, String telefone, String cpf) {
    }

    protected DadosComuns dadosComuns(Map<String, Object> dados) {
        return new DadosComuns(
                Validadores.converterInteiro(dados.get("id_instituicao"), "instituição"),
                Validadores.exigirTexto(dados.get("nome"), "nome", 150, true),
                Validadores.validarEmail(dados.get("email")),
                Validadores.validarTelefone(dados.get("telefone")),
                Validadores.validarCpf(dados.get("cpf")));
    }

    protected <T extends Usuario> T comSenha(T usuario, Map<String, Object> dados) {
        usuario.definirSenha(Validadores.validarSenha(dados.get("senha")));
        return usuario;
    }
}
