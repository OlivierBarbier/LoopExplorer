package fr.lip6.pjava.loopexplore.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class CleanUp implements ICleanUp {

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		return new CleanUpFix(context);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(true, true, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		return new String[] {};
	}

	@Override
	public void setOptions(CleanUpOptions cleanUpOptions) {}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor progressMonitor) throws CoreException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject iJavaProject, ICompilationUnit[] iCompilationUnits, IProgressMonitor iProgressMonitor)
			throws CoreException {
		RefactoringStatus refactoringStatus = new RefactoringStatus();
		/*
		iProgressMonitor.subTask("Checking PreConditions");
		try {
			Thread.sleep(10_000);
		} catch (InterruptedException e) {}
		iProgressMonitor.done();
		*/
		/*
		for (ICompilationUnit iCompilationUnit : iCompilationUnits) {
			refactoringStatus.addFatalError(iCompilationUnit.getElementName() + ": Fatal Error!");
		}
		*/
		return refactoringStatus;
	}

}
