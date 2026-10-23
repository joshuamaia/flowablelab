package com.flowablelab.application.model;

/**
 * Paginação e ordenação independentes de framework (página iniciando em zero).
 *
 * @param sortBy    campo de ordenação; cada adaptador decide quais campos aceita
 * @param ascending direção da ordenação
 */
public record PageQuery(int page, int size, String sortBy, boolean ascending) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? 20 : Math.min(size, MAX_SIZE);
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, null, false);
    }

    public int offset() {
        return page * size;
    }
}
