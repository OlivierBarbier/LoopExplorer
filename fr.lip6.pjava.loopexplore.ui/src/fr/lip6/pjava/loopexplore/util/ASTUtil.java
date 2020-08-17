package fr.lip6.pjava.loopexplore.util;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;

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
		setLambdaBody(lambdaExpression, lambdaBody, ast);
		
		return lambdaExpression;
	}

	public static void setAST(AST ast) {
		ASTUtil.ast = ast;
	}
	
	public static  ITypeBinding resolveITypeBindingFor(String qualifiedClassName, IJavaProject javaProject) throws JavaModelException {
		// Java 8 setting by default, but this setting is overruled by setProject below so irrelevant anyway.
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		// parse java files
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// actually this overrides a bunch of settings, adds the correct classpath etc... to the underlying ASTresolver/JDT compiler.
		parser.setProject(javaProject);
		// Important non default setting : resolve name/type bindings for us ! yes, please, thanks !
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		
		ITypeRoot itr = javaProject.findType(qualifiedClassName).getTypeRoot();
		parser.setSource(itr);
		parser.setResolveBindings(true);
		CompilationUnit node = (CompilationUnit)parser.createAST(null);
		return ((TypeDeclaration) node.types().get(0)).resolveBinding();
	}

	public static void setLambdaBody(LambdaExpression lambda, ASTNode body, AST ast) {
		if (body instanceof Block) {
			lambda.setBody(ASTNodes.copySubtree(ast, body));
		} else if (body instanceof ExpressionStatement){
			// ExpressionStatement
			lambda.setBody(ASTNodes.copySubtree(ast, ((ExpressionStatement) body).getExpression()));								
		} else {
			// add a block
			Block block = (Block) ast.createInstance(ASTNode.BLOCK);
			block.statements().add(ASTNodes.copySubtree(ast, body));
			lambda.setBody(block);											
		}
	}

}
