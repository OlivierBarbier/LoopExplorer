package fr.lip6.pjava.loopexplore.refactorable;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class RefactorableIterable implements IRefactorabeExpression {

	private Expression expr;

	public RefactorableIterable(Expression expr) {
		this.expr = expr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public MethodInvocation refactor() {
		AST ast = expr.getAST();
				
		/* .() */
		MethodInvocation spliterator = ast.newMethodInvocation();
		
		/* .spliterator() */
		spliterator.setName(ast.newSimpleName("spliterator"));
		
		/* expr.spliterator() */
		spliterator.setExpression((Expression) ASTNode.copySubtree(ast, expr));
		
		/* .() */
		MethodInvocation streams = expr.getAST().newMethodInvocation();
		
		/* .stream() */
		streams.setName(ast.newSimpleName("stream"));

		/* java.util.stream.StreamSupport.stream() */
		streams.setExpression(ast.newName(new String[] {
				"java", "util", "stream", "StreamSupport"}));
		
		/* java.util.stream.StreamSupport.stream(expr.spliterator()) */
		streams.arguments().add(spliterator);
		
		/* java.util.stream.StreamSupport.stream(expr.spliterator(), false) */
		streams.arguments().add(ast.newBooleanLiteral(false));

		/* java.util.stream.StreamSupport.stream(expr.spliterator(), false) */
		return streams;
	}

}
