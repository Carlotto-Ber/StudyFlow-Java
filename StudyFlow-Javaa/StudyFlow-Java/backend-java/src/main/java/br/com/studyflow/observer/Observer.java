package br.com.studyflow.observer;

import br.com.studyflow.model.Publicacao;

/** Padrão OBSERVER: quem quer ser avisado quando uma publicação é criada ou alterada. */
public interface Observer {
    void atualizar(Publicacao publicacao, Evento evento);
}
