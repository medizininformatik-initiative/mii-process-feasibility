package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hl7.fhir.r4.model.Task.TaskStatus.FAILED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RateLimitExceededTaskRejecterTest {

    @Captor private ArgumentCaptor<CodeableConcept> reasonCaptor;

    @Mock private DsfClientProvider clientProvider;
    @Mock private TaskHelper taskHelper;
    @Mock private Task task;
    @Mock private ProcessPluginApi api;
    @Mock private Variables variables;

    @InjectMocks private RateLimitExceededTaskRejecter service;


    @Test
    @DisplayName("status and status reason is set on task")
    void taskStatusIsSet() throws Exception {
        when(variables.getStartTask()).thenReturn(task);
        when(task.setStatus(FAILED)).thenReturn(task);

        service.execute(api, variables);

        verify(task).setStatusReason(reasonCaptor.capture());
        assertThat(reasonCaptor.getValue().getText(), is("The request rate limit has been exceeded."));
    }
}
