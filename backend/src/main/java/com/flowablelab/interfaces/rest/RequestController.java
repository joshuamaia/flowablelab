package com.flowablelab.interfaces.rest;

import com.flowablelab.application.model.Filters.RequestFilter;
import com.flowablelab.application.model.PageQuery;
import com.flowablelab.application.usecase.RequestQueryUseCase;
import com.flowablelab.domain.request.RequestStatus;
import com.flowablelab.domain.request.RequestType;
import com.flowablelab.interfaces.rest.dto.Responses.PageResponse;
import com.flowablelab.interfaces.rest.dto.Responses.RequestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
@Tag(name = "Solicitações", description = "Solicitações de negócio correlacionadas às instâncias BPMN")
public class RequestController {

    private final RequestQueryUseCase requests;

    public RequestController(RequestQueryUseCase requests) {
        this.requests = requests;
    }

    @GetMapping
    @Operation(summary = "Lista solicitações com filtros, paginação, ordenação e etapa atual")
    public PageResponse<RequestResponse> list(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) String requester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "openedAt, number, status, type ou requester")
            @RequestParam(defaultValue = "openedAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return PageResponse.of(requests.search(new RequestFilter(status, type, requester),
                new PageQuery(page, size, sort, "asc".equalsIgnoreCase(direction))), RequestResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma solicitação")
    public RequestResponse get(@PathVariable UUID id) {
        return RequestResponse.from(requests.get(id));
    }
}
