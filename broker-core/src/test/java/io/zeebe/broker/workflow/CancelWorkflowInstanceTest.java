/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.workflow;

import static io.zeebe.broker.workflow.data.WorkflowInstanceRecord.PROP_WORKFLOW_ACTIVITY_ID;
import static io.zeebe.broker.workflow.data.WorkflowInstanceRecord.PROP_WORKFLOW_BPMN_PROCESS_ID;
import static io.zeebe.broker.workflow.data.WorkflowInstanceRecord.PROP_WORKFLOW_INSTANCE_KEY;
import static io.zeebe.broker.workflow.data.WorkflowInstanceRecord.PROP_WORKFLOW_VERSION;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import io.zeebe.broker.test.EmbeddedBrokerRule;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.instance.WorkflowDefinition;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.protocol.intent.JobIntent;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;
import io.zeebe.test.broker.protocol.clientapi.ClientApiRule;
import io.zeebe.test.broker.protocol.clientapi.ExecuteCommandResponse;
import io.zeebe.test.broker.protocol.clientapi.SubscribedRecord;
import io.zeebe.test.broker.protocol.clientapi.TestTopicClient;

public class CancelWorkflowInstanceTest
{
    private static final WorkflowDefinition WORKFLOW = Bpmn.createExecutableWorkflow("process")
            .startEvent()
            .serviceTask("task", t -> t.taskType("test").taskRetries(5))
            .endEvent()
            .done();

    public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();
    public ClientApiRule apiRule = new ClientApiRule();

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(brokerRule).around(apiRule);

    private TestTopicClient testClient;

    @Before
    public void init()
    {
        testClient = apiRule.topic();
    }

    @Test
    public void shouldCancelWorkflowInstance()
    {
        // given
        testClient.deploy(WORKFLOW);

        final long workflowInstanceKey = testClient.createWorkflowInstance("process");

        testClient.receiveEvents()
            .withIntent(WorkflowInstanceIntent.ACTIVITY_ACTIVATED)
            .getFirst();

        // when
        final ExecuteCommandResponse response = cancelWorkflowInstance(workflowInstanceKey);

        // then
        assertThat(response.intent()).isEqualTo(WorkflowInstanceIntent.CANCELED);

        final SubscribedRecord workflowInstanceCanceledEvent = testClient.receiveEvents()
            .ofTypeWorkflowInstance()
            .withIntent(WorkflowInstanceIntent.CANCELED)
            .getFirst();

        assertThat(workflowInstanceCanceledEvent.key()).isEqualTo(workflowInstanceKey);
        assertThat(workflowInstanceCanceledEvent.value())
            .containsEntry(PROP_WORKFLOW_BPMN_PROCESS_ID, "process")
            .containsEntry(PROP_WORKFLOW_VERSION, 1)
            .containsEntry(PROP_WORKFLOW_INSTANCE_KEY, workflowInstanceKey)
            .containsEntry(PROP_WORKFLOW_ACTIVITY_ID, "");

        final List<SubscribedRecord> workflowEvents = testClient
                .receiveRecords()
                .ofTypeWorkflowInstance()
                .limit(9)
                .collect(Collectors.toList());

        assertThat(workflowEvents).extracting(e -> e.intent()).containsExactly(
                WorkflowInstanceIntent.CREATE,
                WorkflowInstanceIntent.CREATED,
                WorkflowInstanceIntent.START_EVENT_OCCURRED,
                WorkflowInstanceIntent.SEQUENCE_FLOW_TAKEN,
                WorkflowInstanceIntent.ACTIVITY_READY,
                WorkflowInstanceIntent.ACTIVITY_ACTIVATED,
                WorkflowInstanceIntent.CANCEL,
                WorkflowInstanceIntent.ACTIVITY_TERMINATED,
                WorkflowInstanceIntent.CANCELED);
    }

    @Test
    public void shouldCancelActivityInstance()
    {
        // given
        testClient.deploy(WORKFLOW);

        final long workflowInstanceKey = testClient.createWorkflowInstance("process");

        final SubscribedRecord activityActivatedEvent = testClient
                .receiveEvents()
                .withIntent(WorkflowInstanceIntent.ACTIVITY_ACTIVATED)
                .getFirst();

        final ExecuteCommandResponse response = cancelWorkflowInstance(workflowInstanceKey);

        // then
        assertThat(response.intent()).isEqualTo(WorkflowInstanceIntent.CANCELED);

        final SubscribedRecord activityTerminatedEvent = testClient
            .receiveEvents()
            .withIntent(WorkflowInstanceIntent.ACTIVITY_TERMINATED)
            .getFirst();

        assertThat(activityTerminatedEvent.key()).isEqualTo(activityActivatedEvent.key());
        assertThat(activityTerminatedEvent.value())
            .containsEntry(PROP_WORKFLOW_BPMN_PROCESS_ID, "process")
            .containsEntry(PROP_WORKFLOW_VERSION, 1)
            .containsEntry(PROP_WORKFLOW_INSTANCE_KEY, workflowInstanceKey)
            .containsEntry(PROP_WORKFLOW_ACTIVITY_ID, "task");
    }

    @Test
    public void shouldCancelJobForActivity()
    {
        // given
        testClient.deploy(WORKFLOW);

        final long workflowInstanceKey = testClient.createWorkflowInstance("process");

        final SubscribedRecord jobCreatedEvent = testClient
            .receiveEvents()
            .ofTypeJob()
            .withIntent(JobIntent.CREATED)
            .getFirst();

        final ExecuteCommandResponse response = cancelWorkflowInstance(workflowInstanceKey);

        // then
        assertThat(response.intent()).isEqualTo(WorkflowInstanceIntent.CANCELED);

        final SubscribedRecord jobCanceledEvent = testClient
            .receiveEvents()
            .ofTypeJob()
            .withIntent(JobIntent.CANCELED)
            .getFirst();

        assertThat(jobCanceledEvent.key()).isEqualTo(jobCreatedEvent.key());

        @SuppressWarnings("unchecked")
        final Map<String, Object> headers = (Map<String, Object>) jobCanceledEvent.value().get("headers");
        assertThat(headers)
            .containsEntry("workflowInstanceKey", workflowInstanceKey)
            .containsEntry("bpmnProcessId", "process")
            .containsEntry("workflowDefinitionVersion", 1)
            .containsEntry("activityId", "task");
    }

    @Test
    public void shouldRejectCancelNonExistingWorkflowInstance()
    {
        // when
        final ExecuteCommandResponse response = cancelWorkflowInstance(-1L);

        // then
        assertThat(response.recordType()).isEqualTo(RecordType.COMMAND_REJECTION);

        final SubscribedRecord cancelRejection = testClient
            .receiveRejections()
            .ofTypeWorkflowInstance()
            .withIntent(WorkflowInstanceIntent.CANCEL)
            .getFirst();

        assertThat(cancelRejection).isNotNull();
    }

    @Test
    public void shouldRejectCancelCompletedWorkflowInstance()
    {
        // given
        testClient.deploy(Bpmn.createExecutableWorkflow("process")
                .startEvent()
                .endEvent()
                .done());

        final long workflowInstanceKey = testClient.createWorkflowInstance("process");

        testClient
            .receiveEvents()
            .ofTypeWorkflowInstance()
            .withIntent(WorkflowInstanceIntent.COMPLETED)
            .getFirst();

        // when
        final ExecuteCommandResponse response = cancelWorkflowInstance(workflowInstanceKey);

        // then
        assertThat(response.recordType()).isEqualTo(RecordType.COMMAND_REJECTION);

        final SubscribedRecord cancelRejection = testClient
            .receiveRejections()
            .ofTypeWorkflowInstance()
            .withIntent(WorkflowInstanceIntent.CANCEL)
            .getFirst();

        assertThat(cancelRejection).isNotNull();
    }

    private ExecuteCommandResponse cancelWorkflowInstance(final long workflowInstanceKey)
    {
        return apiRule.createCmdRequest()
            .type(ValueType.WORKFLOW_INSTANCE, WorkflowInstanceIntent.CANCEL)
            .key(workflowInstanceKey)
            .command()
            .done()
            .sendAndAwait();
    }
}
