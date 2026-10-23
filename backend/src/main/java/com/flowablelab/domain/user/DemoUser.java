package com.flowablelab.domain.user;

import com.flowablelab.domain.exception.UserNotAllowedException;

import java.util.Arrays;
import java.util.Collection;

/**
 * Usuários demonstrativos. NÃO é um mecanismo de autenticação: o perfil é escolhido
 * livremente no navegador e serve apenas para ilustrar responsáveis diferentes por tarefa.
 */
public enum DemoUser {

    COLABORADOR("colaborador", "Carla Colaboradora", "colaborador"),
    GESTOR("gestor", "Gabriel Gestor", "gestor"),
    TI("ti", "Tiago da TI", "ti");

    private final String id;
    private final String displayName;
    private final String group;

    DemoUser(String id, String displayName, String group) {
        this.id = id;
        this.displayName = displayName;
        this.group = group;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String group() {
        return group;
    }

    public static DemoUser fromId(String id) {
        return Arrays.stream(values())
                .filter(user -> user.id.equalsIgnoreCase(id == null ? "" : id.trim()))
                .findFirst()
                .orElseThrow(() -> new UserNotAllowedException(
                        "Perfil demonstrativo '%s' desconhecido. Use colaborador, gestor ou ti.".formatted(id)));
    }

    /** Elegível quando é o responsável direto ou pertence a um dos grupos candidatos. */
    public boolean canWorkOn(String assignee, Collection<String> candidateGroups) {
        if (assignee != null && !assignee.isBlank()) {
            return id.equals(assignee);
        }
        return candidateGroups != null && candidateGroups.contains(group);
    }
}
