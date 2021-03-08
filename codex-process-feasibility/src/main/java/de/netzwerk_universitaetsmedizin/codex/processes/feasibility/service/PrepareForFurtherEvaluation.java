package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;


/**
 * The type prepare query for further evaluation
 */
public class PrepareForFurtherEvaluation extends AbstractServiceDelegate implements InitializingBean {
  
  
  /**
   * Instantiates a new Prepare for further evaluation.
   *
   * @param clientProvider the client provider
   * @param taskHelper     the task helper
   */
  public PrepareForFurtherEvaluation(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
    super(clientProvider, taskHelper);
  }
 
  @Override
  protected void doExecute(DelegateExecution execution) throws BpmnError, Exception {
    for (Map.Entry<Reference, MeasureReport> entry : getMeasureReportMap(execution).entrySet()){
      getLeadingTaskFromExecutionVariables().addOutput(
          addOrganizationReference(createMeasureReportReferenceOutput(entry.getValue()), entry.getKey()));
    }
  }
  
  private HashMap<Reference, MeasureReport> getMeasureReportMap(DelegateExecution execution) {
    HashMap<Reference, MeasureReport> measureReports = (HashMap<Reference, MeasureReport>) execution.getVariable(ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP);
    return measureReports != null ? measureReports :  new HashMap<Reference, MeasureReport>();
  }
  
  private Task.TaskOutputComponent createMeasureReportReferenceOutput(MeasureReport measureReport) {
    return getTaskHelper().createOutput(ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE,
        new Reference().setReference("MeasureReport/" + measureReport.getIdElement().getIdPart()));
  }
  
  private Task.TaskOutputComponent addOrganizationReference(Task.TaskOutputComponent output, Reference organization){
    output.addExtension(ConstantsFeasibility.EXTENSION_DIC_URI, organization);
    return output;
  }
}
