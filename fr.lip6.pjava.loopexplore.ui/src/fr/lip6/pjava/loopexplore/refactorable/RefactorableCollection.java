package fr.lip6.pjava.loopexplore.refactorable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class RefactorableCollection implements IRefactorabeExpression {

	private Expression expr;

	public RefactorableCollection(Expression expr) {
		this.expr = expr;
	}

	@Override
	public MethodInvocation refactor() {
		/* .() */
		MethodInvocation streams = expr.getAST().newMethodInvocation();
		
		/* .stream() */
		streams.setName(expr.getAST().newSimpleName("stream"));
		
		/* expr.stream() */
		streams.setExpression((Expression) ASTNode.copySubtree(expr.getAST(), expr));
		
		
		return streams;
	}
}
