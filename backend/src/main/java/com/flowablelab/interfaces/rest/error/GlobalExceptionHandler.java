package com.flowablelab.interfaces.rest.error;

import com.flowablelab.domain.exception.BusinessRuleViolationException;
import com.flowablelab.domain.exception.InvalidTaskActionException;
import com.flowablelab.domain.exception.ResourceNotFoundException;
import com.flowablelab.domain.exception.TaskAlreadyCompletedException;
import com.flowablelab.domain.exception.UserNotAllowedException;
import com.flowablelab.domain.exception.WorkflowExecutionException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidTypeIdException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Converte exceções em respostas RFC 9457 (Problem Details). Mensagens técnicas e stack
 * traces ficam apenas no log; o cliente recebe uma descrição segura e um tipo estável.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_BASE = "https://flowablelab.dev/problems/";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail notFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(FlowableObjectNotFoundException.class)
    public ProblemDetail engineObjectNotFound(FlowableObjectNotFoundException ex) {
        log.info("Objeto não encontrado no motor: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "not-found", "Recurso não encontrado",
                "O recurso solicitado não existe no motor de processos.");
    }

    @ExceptionHandler(TaskAlreadyCompletedException.class)
    public ProblemDetail taskCompleted(TaskAlreadyCompletedException ex) {
        return problem(HttpStatus.CONFLICT, "task-already-completed", "Tarefa já concluída", ex.getMessage());
    }

    @ExceptionHandler(UserNotAllowedException.class)
    public ProblemDetail notAllowed(UserNotAllowedException ex) {
        return problem(HttpStatus.FORBIDDEN, "user-not-allowed", "Usuário não autorizado para a tarefa",
                ex.getMessage());
    }

    @ExceptionHandler(InvalidTaskActionException.class)
    public ProblemDetail invalidAction(InvalidTaskActionException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-task-action", "Ação inválida para a tarefa",
                ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail businessRule(BusinessRuleViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "business-rule", "Regra de negócio violada",
                ex.getMessage());
    }

    @ExceptionHandler(WorkflowExecutionException.class)
    public ProblemDetail workflowFailure(WorkflowExecutionException ex) {
        log.error("Falha de execução no workflow", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "workflow-execution",
                "Falha na execução do processo",
                "Uma etapa automática do processo falhou. A operação foi desfeita; consulte os logs do servidor.");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail concurrentUpdate(OptimisticLockingFailureException ex) {
        log.warn("Conflito de concorrência: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "concurrent-update", "Atualização concorrente",
                "O recurso foi alterado por outra operação. Atualize os dados e tente novamente.");
    }

    @ExceptionHandler({CannotGetJdbcConnectionException.class, CannotCreateTransactionException.class,
            DataAccessResourceFailureException.class})
    public ProblemDetail databaseUnavailable(Exception ex) {
        log.error("Banco de dados indisponível", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "database-unavailable", "Banco de dados indisponível",
                "Não foi possível acessar o banco de dados. Tente novamente em instantes.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail typeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-parameter", "Parâmetro inválido",
                "Valor inválido para o parâmetro '%s'.".formatted(ex.getName()));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception ex) {
        log.error("Erro inesperado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Erro interno",
                "Ocorreu um erro inesperado. Consulte os logs do servidor.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", "Dados inválidos",
                "A requisição possui campos inválidos.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidTypeIdException typeId) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(HttpStatus.NOT_FOUND,
                    "process-not-found", "Processo inexistente",
                    "Não existe processo com a chave '%s'.".formatted(typeId.getTypeId())));
        }
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "malformed-request",
                "Requisição malformada", "O corpo da requisição é inválido ou está incompleto."));
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_BASE + type));
        problem.setTitle(title);
        return problem;
    }
}
