package handler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import analyzer.EnhancedForLoopAnalyzer;
import collector.EnhancedForLoopCollector;



public class CommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		/* <CUSTOM CODE HERE> */
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		List<?> list = SelectionUtil.toList(selection);
		List<IJavaProject> javaProjects = list.stream()
				.filter(e -> e instanceof IJavaProject)
				.map(e -> (IJavaProject) e)
				.collect(Collectors.toList());

		List<String> resultsHeader = new ArrayList<>(Arrays.asList(
				"Project", "#SuperFor", "#SubFor", "#Break", "#Continue", "#Return", "#Throw", "#nfnef", "Iterable", "Path", "From", "To", "Refactorable"
				));

		List<String> refactoringResultsHeader = new ArrayList<>(Arrays.asList(
				"Original", "Refactoring"
				));	

		javaProjects.parallelStream().forEach(javaProject -> 
		{
			CSVPrinter resultsPrinter = null;
			CSVPrinter refactoringResultsPrinter = null;
			try {
				String folder = javaProject.getProject().getLocation().toFile().getCanonicalPath();
				resultsPrinter = new CSVPrinter(
						new FileWriter(folder+"/loop-explorer-results.csv", false),
						CSVFormat.EXCEL.withHeader(resultsHeader.toArray(new String[resultsHeader.size()]))
						);

				refactoringResultsPrinter = new CSVPrinter(
						new FileWriter(folder + "/loop-explorer-refacto-results.csv", false),
						CSVFormat.EXCEL.withHeader(refactoringResultsHeader.toArray(new String[refactoringResultsHeader.size()]))
						);

			} catch (IOException e1) {			e1.printStackTrace();}

			// AbstractMetricSource metricSource = Dispatcher.getAbstractMetricSource(javaProject);
			// Metric value = metricSource.getValue(Constants.TLOC);

			EnhancedForLoopCollector eflCollector = new EnhancedForLoopCollector(javaProject);
			eflCollector.collect();

			int size = eflCollector.enhancedForStatementSet.size();
			System.out.println("---");
			System.out.println("size: "+size);
			System.out.println("---");


			try
			{
				List<EnhancedForLoopAnalyzer> eflas = eflCollector.enhancedForStatementSet
						.stream()
						//.filter(efla -> efla.getNumberOfBreakStatements() == 0)
						//.filter(efla -> efla.getNumberOfContinueStatements() == 0)
						//.filter(efla -> efla.getNumberOfNeitherFinalNorEffectivelyFinalVariables() == 0)
						//.filter(efla -> efla.getNumberOfReturnStatements() == 0)
						//.filter(efla -> efla.getNumberOfSubForStatements() == 0)
						//.filter(efla -> efla.getNumberOfSuperForStatements() == 0)
						.collect(Collectors.toList());
				for(EnhancedForLoopAnalyzer efla : eflas)
				{
					resultsPrinter.print(efla.getProjectName());
					resultsPrinter.print(efla.getNumberOfSuperForStatements());
					resultsPrinter.print(efla.getNumberOfSubForStatements());
					resultsPrinter.print(efla.getNumberOfBreakStatements());
					resultsPrinter.print(efla.getNumberOfContinueStatements());
					resultsPrinter.print(efla.getNumberOfReturnStatements());
					resultsPrinter.print(efla.getNumberOfThrowStatements());
					resultsPrinter.print(efla.getNumberOfNeitherFinalNorEffectivelyFinalVariables());
					resultsPrinter.print(efla.getIterableClassName());
					resultsPrinter.print(efla.getPath());
					resultsPrinter.print(efla.getStartLine());
					resultsPrinter.print(efla.getEndLine());
					resultsPrinter.print(efla.isRefactorable() ? "Yes" : "No");
					resultsPrinter.println();

					if (efla.isRefactorable()) {
						refactoringResultsPrinter.print(efla.efs.toString());
						refactoringResultsPrinter.print(efla.getRefactoring());
						refactoringResultsPrinter.println();
					}
				}
				resultsPrinter.close();
				refactoringResultsPrinter.close();
			}
			catch (IOException e1)
			{
				e1.printStackTrace();
			}

		});

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				"Handler",
				"Hello, Eclipse world");
		return null;
	}

}
