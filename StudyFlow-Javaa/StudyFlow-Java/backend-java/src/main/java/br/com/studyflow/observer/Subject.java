package br.com.studyflow.observer;

/** Padrão OBSERVER: quem mantém a lista de observers e os avisa. */
public interface Subject {
    void anexar(Observer observer);

    void desanexar(Observer observer);

    void notificar(Evento evento);
}
