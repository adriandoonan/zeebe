package io.zeebe.broker.system.deployment.processor;

import java.util.Iterator;

import io.zeebe.broker.logstreams.processor.*;
import io.zeebe.broker.system.deployment.data.DeploymentPositionByWorkflowKey;
import io.zeebe.broker.system.deployment.data.WorkflowKeyByProcessIdAndVersion;
import io.zeebe.broker.workflow.data.DeployedWorkflow;
import io.zeebe.broker.workflow.data.DeploymentEvent;
import io.zeebe.msgpack.value.ValueArray;

public class DeploymentCreatedEventProcess implements TypedEventProcessor<DeploymentEvent>
{
    private final DeploymentPositionByWorkflowKey deploymentPositionByWorkflowKey;
    private final WorkflowKeyByProcessIdAndVersion workflowKeyByProcessIdAndVersion;

    public DeploymentCreatedEventProcess(DeploymentPositionByWorkflowKey deploymentPositionByWorkflowKey,
        WorkflowKeyByProcessIdAndVersion workflowKeyByProcessIdAndVersion)
    {
        this.deploymentPositionByWorkflowKey = deploymentPositionByWorkflowKey;
        this.workflowKeyByProcessIdAndVersion = workflowKeyByProcessIdAndVersion;
    }

    @Override
    public boolean executeSideEffects(TypedEvent<DeploymentEvent> event, TypedResponseWriter responseWriter)
    {
        return responseWriter.write(event);
    }

    @Override
    public void updateState(TypedEvent<DeploymentEvent> event)
    {
        final DeploymentEvent deploymentEvent = event.getValue();

        final ValueArray<DeployedWorkflow> deployedWorkflows = deploymentEvent.deployedWorkflows();

        final Iterator<DeployedWorkflow> iterator = deployedWorkflows.iterator();

        while (iterator.hasNext())
        {
            final DeployedWorkflow deployedWorkflow = iterator.next();
            deploymentPositionByWorkflowKey.set(deployedWorkflow.getKey(), event.getPosition());
            workflowKeyByProcessIdAndVersion.set(deployedWorkflow.getBpmnProcessId(), deployedWorkflow.getVersion(), deployedWorkflow.getKey());
        }
    }
}
