package fr.lip6.pjava.loopexplore.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.fix.AbstractMultiFix;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

public class RefactorLoops extends AbstractMultiFix {

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}


}
