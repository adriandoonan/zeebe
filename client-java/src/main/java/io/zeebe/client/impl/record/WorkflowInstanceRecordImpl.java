/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.client.impl.record;

import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.zeebe.client.api.record.WorkflowInstanceRecord;
import io.zeebe.client.api.record.ZeebeObjectMapper;
import io.zeebe.client.impl.data.PayloadField;
import io.zeebe.client.impl.event.WorkflowInstanceEventImpl;
import io.zeebe.protocol.clientapi.RecordType;
import io.zeebe.protocol.clientapi.ValueType;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;

public abstract class WorkflowInstanceRecordImpl extends RecordImpl implements WorkflowInstanceRecord
{
    private String bpmnProcessId;
    private int version = -1;
    private long workflowKey = -1L;
    private long workflowInstanceKey = -1L;
    private String activityId;
    private PayloadField payload;

    public WorkflowInstanceRecordImpl(ZeebeObjectMapper objectMapper, RecordType recordType)
    {
        super(objectMapper, recordType, ValueType.WORKFLOW_INSTANCE);
    }

    public WorkflowInstanceRecordImpl(WorkflowInstanceRecordImpl base, WorkflowInstanceIntent intent)
    {
        super(base, intent);

        this.bpmnProcessId = base.getBpmnProcessId();
        this.version = base.getVersion();
        this.workflowKey = base.getWorkflowKey();
        this.workflowInstanceKey = base.getWorkflowInstanceKey();
        this.activityId = base.getActivityId();
        this.payload = new PayloadField(base.payload);
    }

    @Override
    public String getBpmnProcessId()
    {
        return bpmnProcessId;
    }

    public void setBpmnProcessId(String bpmnProcessId)
    {
        this.bpmnProcessId = bpmnProcessId;
    }

    @Override
    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    @Override
    public long getWorkflowInstanceKey()
    {
        return workflowInstanceKey;
    }

    public void setWorkflowInstanceKey(long workflowInstanceKey)
    {
        this.workflowInstanceKey = workflowInstanceKey;
    }

    @Override
    public String getActivityId()
    {
        return activityId;
    }

    public void setActivityId(String activityId)
    {
        this.activityId = activityId;
    }

    @JsonProperty("payload")
    public PayloadField getPayloadField()
    {
        return payload;
    }

    @JsonProperty("payload")
    public void setPayloadField(PayloadField payload)
    {
        this.payload = payload;
    }

    @Override
    public String getPayload()
    {
        return payload.getAsJsonString();
    }

    public void setPayload(String jsonString)
    {
        this.payload.setJson(jsonString);
    }

    public void setPayload(InputStream jsonStream)
    {
        this.payload.setJson(jsonStream);
    }

    @Override
    public long getWorkflowKey()
    {
        return workflowKey;
    }

    public void setWorkflowKey(long workflowKey)
    {
        this.workflowKey = workflowKey;
    }

    @Override
    public Class<? extends RecordImpl> getEventClass()
    {
        return WorkflowInstanceEventImpl.class;
    }

}
