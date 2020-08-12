package fr.lip6.pjava.loopexplore.ui;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.jdt.core.IJavaProject;

import fr.lip6.pjava.loopexplore.analyzer.EnhancedForLoopAnalyzer;
import fr.lip6.pjava.loopexplore.collector.EnhancedForLoopCollector;

public class Refactor {

	public static void refactorProjects(List<IJavaProject> javaProjects) {
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
						new FileWriter(folder+"/" + javaProject.getElementName() + ".loop-explorer-results.csv", false),
						CSVFormat.EXCEL.withHeader(resultsHeader.toArray(new String[resultsHeader.size()]))
						);

				refactoringResultsPrinter = new CSVPrinter(
						new FileWriter(folder + "/" + javaProject.getElementName() +".loop-explorer-refacto-results.csv", false),
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
	}

}
