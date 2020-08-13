package fr.lip6.pjava.loopexplore.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class CleanUpFix implements ICleanUpFix {

	final private CompilationUnit compilationUnit;
	final private ICompilationUnit sourceDocument;

	public CleanUpFix(CleanUpContext context) {
		compilationUnit = context.getAST();
		sourceDocument  = (ICompilationUnit)compilationUnit.getJavaElement();
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		final ASTRewrite rewriter = ASTRewrite.create(compilationUnit.getAST());
		
		final CompilationUnitChange compilationUnitChange = new CompilationUnitChange(
			sourceDocument.getElementName(), sourceDocument);

		compilationUnit.accept(new ASTVisitor() {
			@Override
			public void endVisit(IfStatement currentIdStatement) {
				super.endVisit(currentIdStatement);
				/* Toy Rewrite: Just To Prove The CleanUp Is Functional */
				if (currentIdStatement.getElseStatement() == null) {
					IfStatement newIfStatement = rewriter.getAST().newIfStatement();
					newIfStatement.setExpression((Expression) rewriter.createMoveTarget(currentIdStatement.getExpression()));
					newIfStatement.setThenStatement((Statement) rewriter.createMoveTarget(currentIdStatement.getThenStatement()));
					newIfStatement.setElseStatement(rewriter.getAST().newEmptyStatement());
					rewriter.replace(currentIdStatement, newIfStatement, null);
				}
			}
		});

		compilationUnitChange.setEdit(rewriter.rewriteAST());
		return compilationUnitChange;
	}
}
