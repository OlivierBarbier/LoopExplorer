package refactorable;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class RefactorableArray implements IRefactorabeExpression {

	private Expression expr;

	public RefactorableArray(Expression expr) {
		this.expr = expr;
	}

	@Override
	@SuppressWarnings("unchecked")
	public MethodInvocation refactor() {
		/* .() */
		MethodInvocation streams = expr.getAST().newMethodInvocation();
		
		/* .stream() */
		streams.setName(expr.getAST().newSimpleName("stream"));
		
		/* java.util.Arrays.stream() */
		streams.setExpression(expr.getAST().newName(new String[]{"java", "util", "Arrays"}));
		
		/* java.util.Arrays.stream(expr) */
		streams.arguments().add(ASTNode.copySubtree(expr.getAST(), expr));
		
		return streams;
	}

}
