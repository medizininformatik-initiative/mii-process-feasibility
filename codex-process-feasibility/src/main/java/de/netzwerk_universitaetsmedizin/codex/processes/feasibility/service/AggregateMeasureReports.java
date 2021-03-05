package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import java.util.HashMap;
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
 * The type Aggregate measure reports
 *
 */
public class AggregateMeasureReports extends AbstractServiceDelegate implements InitializingBean {
  
  /**
   * Instantiates a new Aggregate measure reports.
   *
   * @param clientProvider the client provider
   * @param taskHelper     the task helper
   */
  public AggregateMeasureReports(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper) {
    super(clientProvider, taskHelper);
  }
 
  @Override
  protected void doExecute(DelegateExecution execution) throws BpmnError, Exception {
    Task task = getCurrentTaskFromExecutionVariables();
    HashMap<Reference, MeasureReport> measureReports = getMeasureReportMap(execution);
    measureReports.put(task.getRequester(), getMeasureReport(execution));
    execution.setVariable(ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP, measureReports);
  }
  
  private MeasureReport getMeasureReport(DelegateExecution execution) {
    return (MeasureReport) execution.getVariable(ConstantsFeasibility.VARIABLE_MEASURE_REPORT);
  }
  
  private  HashMap<Reference, MeasureReport> getMeasureReportMap(DelegateExecution execution) {
    HashMap<Reference, MeasureReport> measureReports = ( HashMap<Reference, MeasureReport>) execution.getVariable(ConstantsFeasibility.VARIABLE_MEASURE_REPORT_MAP);
   return measureReports != null ? measureReports : new HashMap<Reference, MeasureReport>();
  }
}
