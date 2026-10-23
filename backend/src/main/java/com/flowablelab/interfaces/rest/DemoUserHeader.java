package com.flowablelab.interfaces.rest;

/**
 * Cabeçalho com o perfil demonstrativo selecionado no frontend.
 * Não é autenticação: qualquer cliente pode enviar qualquer valor.
 */
public final class DemoUserHeader {

    public static final String NAME = "X-Demo-User";
    public static final String DESCRIPTION =
            "Perfil demonstrativo (colaborador, gestor ou ti). NÃO é um mecanismo de autenticação.";

    private DemoUserHeader() {
    }
}
