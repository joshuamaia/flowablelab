package com.flowablelab.infrastructure.flowable;

import com.flowablelab.application.usecase.RequestLifecycleUseCase;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.job.api.Job;

/**
 * Quando um job assíncrono esgota as tentativas e vai para a fila de dead letter, marca a
 * solicitação de negócio como FAILED (dentro da transação do próprio motor).
 */
public class JobFailureListener implements FlowableEventListener {

    private final RequestLifecycleUseCase lifecycle;

    public JobFailureListener(RequestLifecycleUseCase lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event.getType() == FlowableEngineEventType.JOB_MOVED_TO_DEADLETTER
                && event instanceof FlowableEngineEntityEvent entityEvent
                && entityEvent.getEntity() instanceof Job job
                && job.getProcessInstanceId() != null) {
            lifecycle.recordFailure(job.getProcessInstanceId(), job.getElementId(), job.getExceptionMessage());
        }
    }

    @Override
    public boolean isFailOnException() {
        // Uma falha ao atualizar a solicitação não deve impedir o motor de registrar o dead letter.
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}
