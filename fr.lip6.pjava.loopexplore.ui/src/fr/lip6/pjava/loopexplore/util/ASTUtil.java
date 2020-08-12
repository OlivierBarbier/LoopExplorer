package fr.lip6.pjava.loopexplore.util;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class ASTUtil {

	private static AST ast;
		
	@SuppressWarnings("unchecked")
	public static MethodInvocation newMethodInvocation(Expression object, String methodName,
			Expression methodArgument) {

		MethodInvocation methodInvocation = ast.newMethodInvocation();
		
		methodInvocation.setName(ast.newSimpleName(methodName));
		
		methodInvocation.setExpression(object);
		
		methodInvocation.arguments().add(methodArgument);
		
		return methodInvocation;
	}

	@SuppressWarnings("unchecked")
	public static LambdaExpression newLambdaExpression(ASTNode lamdbaArgument, ASTNode lambdaBody) {
		
		/* () -> ()*/
		LambdaExpression lambdaExpression = ast.newLambdaExpression();
		
		/* (<efs.getParameter()>) -> ()*/
		lambdaExpression.parameters().add(ASTNode.copySubtree(ast, lamdbaArgument));
		
		/*  (<efs.getParameter()>) -> <efs.getBody(>) */
		lambdaExpression.setBody(ASTNode.copySubtree(ast, lambdaBody));
		
		return lambdaExpression;
	}

	public static void setAST(AST ast) {
		ASTUtil.ast = ast;
	}

}
