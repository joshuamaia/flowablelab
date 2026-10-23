package com.flowablelab.domain.exception;

/**
 * Base de todas as exceções de negócio. A camada de interfaces converte cada
 * subtipo em uma resposta Problem Details com o status HTTP adequado.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
