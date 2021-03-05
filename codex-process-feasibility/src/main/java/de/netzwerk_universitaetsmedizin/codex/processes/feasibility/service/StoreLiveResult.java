package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import java.util.List;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.springframework.beans.factory.InitializingBean;

/**
 * The type Store live result.
 */
public class StoreLiveResult extends AbstractServiceDelegate implements InitializingBean {
  
  /**
   * Instantiates a new Store live result.
   *
   * @param clientProvider the client provider
   * @param taskHelper     the task helper
   */
  public StoreLiveResult(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
    super(clientProvider, taskHelper);
  }
  
  @Override
  protected void doExecute(DelegateExecution execution) throws BpmnError, Exception {
    MeasureReport measureReport = getMeasureReport(execution);
    Task task = getCurrentTaskFromExecutionVariables();
    addMeasureReportReferenceToTask(measureReport, task);
    storeMeasureReport(measureReport);
  }
  
  private MeasureReport getMeasureReport(DelegateExecution execution) {
    return (MeasureReport) execution.getVariable(ConstantsFeasibility.VARIABLE_MEASURE_REPORT);
  }
  
  private void addMeasureReportReferenceToTask(MeasureReport measureReport, Task task) {
    task.addOutput(createMeasureReportReferenceOutput(measureReport));
  }
  
  private TaskOutputComponent createMeasureReportReferenceOutput(MeasureReport measureReport) {
    return getTaskHelper().createOutput(ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE,
        new Reference().setReference("MeasureReport/" + measureReport.getIdElement().getIdPart()));
  }
  
  
  
  private IdType storeMeasureReport(MeasureReport measureReport) {
    measureReport.setMeta(
        new Meta().setTag(
            List.of(new Coding()
                    .setSystem("http://highmed.org/fhir/CodeSystem/authorization-role")
                    .setCode("LOCAL"))
            )
    );
    return getFhirWebserviceClientProvider().getLocalWebserviceClient()
        .withMinimalReturn()
        .create(measureReport);
  }
}
