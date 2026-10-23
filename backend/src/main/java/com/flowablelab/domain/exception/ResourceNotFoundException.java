package com.flowablelab.domain.exception;

public class ResourceNotFoundException extends DomainException {

    private final String resource;
    private final String identifier;

    public ResourceNotFoundException(String resource, String identifier) {
        super("%s '%s' não encontrado(a).".formatted(resource, identifier));
        this.resource = resource;
        this.identifier = identifier;
    }

    public String resource() {
        return resource;
    }

    public String identifier() {
        return identifier;
    }
}
