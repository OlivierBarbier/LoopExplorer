package fr.lip6.pjava.loopexplore.ui;

import java.rmi.UnexpectedException;

import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import fr.lip6.pjava.loopexplore.refactorable.IRefactorabeExpression;
import fr.lip6.pjava.loopexplore.refactorable.RefactorableExpressionFactory;
import fr.lip6.pjava.loopexplore.util.ASTUtil;
import fr.lip6.pjava.loopexplore.util.LambdaExceptionUtil;

@SuppressWarnings("restriction")
public class CleanUpFix implements ICleanUpFix {

	final private CompilationUnit compilationUnit;
	final private ICompilationUnit sourceDocument;
	final private ASTRewrite rewriter;
	@SuppressWarnings("unused")
	final private IJavaProject javaProject;
	final private AST ast;
	final private ITypeBinding collectionTypeBinding;

	public CleanUpFix(CleanUpContext context) {
		compilationUnit = context.getAST();
		javaProject = compilationUnit.getJavaElement().getJavaProject();
		sourceDocument  = (ICompilationUnit)compilationUnit.getJavaElement();
		rewriter = ASTRewrite.create(compilationUnit.getAST());
		ast = rewriter.getAST();
		try {
			collectionTypeBinding = ASTUtil.resolveITypeBindingFor("java.util.Collection", javaProject);
		} catch (JavaModelException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		final CompilationUnitChange compilationUnitChange = new CompilationUnitChange(
			sourceDocument.getElementName(), sourceDocument);

		rewriteRule02();

		compilationUnitChange.setEdit(rewriter.rewriteAST());
		return compilationUnitChange;
	}
	
	/* 
	 * Toy Rewrite: Just To Prove The CleanUp Is Functional 
	 * 
	 * if (<expr>) {<then-statement>} -> if (<expr>) {<then-statement>} else ; 
	 *
	 * */	
	private void rewriteRule01() {
		compilationUnit.accept(new ASTVisitor() {
			@Override
			public void endVisit(IfStatement currentIdStatement) {
				if (currentIdStatement.getElseStatement() == null) {
					IfStatement newIfStatement = rewriter.getAST().newIfStatement();
					newIfStatement.setExpression((Expression) rewriter.createMoveTarget(currentIdStatement.getExpression()));
					newIfStatement.setThenStatement((Statement) rewriter.createMoveTarget(currentIdStatement.getThenStatement()));
					
					/*  else ; */
					newIfStatement.setElseStatement(rewriter.getAST().newEmptyStatement());
					
					rewriter.replace(currentIdStatement, newIfStatement, null);
				}
			}
		});		
	}

	private void rewriteRule02() {
		compilationUnit.accept(new ASTVisitor() {
			@SuppressWarnings("unchecked")
			@Override
			public void endVisit(EnhancedForStatement enhancedForStatement) {
				IRefactorabeExpression refactorableExpression = 
						RefactorableExpressionFactory.make(enhancedForStatement.getExpression(), collectionTypeBinding);
				
				final MethodInvocation stream = refactorableExpression.refactor();
				
		
					/* .stream() */
					//final MethodInvocation stream = rewriter.getAST().newMethodInvocation();
					//stream.setName(rewriter.getAST().newSimpleName("stream"));
					
					/* java.util.Arrays.stream() */
					//stream.setExpression(rewriter.getAST().newName(new String[]{"java", "util", "Arrays"}));
					
					/* java.util.Arrays.stream(expr) */
					//stream.arguments().add(ASTNodes.copySubtree(rewriter.getAST(), enhancedForStatement.getExpression()));
					
					
					/*  () -> () */
					final LambdaExpression lambda = rewriter.getAST().newLambdaExpression();
					
					/*  (<enhancedForStatement.getParameter()>) -> <enhancedForStatement.getBody(>) */
					lambda.parameters().add(ASTNodes.copySubtree(rewriter.getAST(), enhancedForStatement.getParameter()));
					lambda.setBody(ASTNodes.copySubtree(rewriter.getAST(), enhancedForStatement.getBody()));					
					
					
					/* .forEach() */
					final MethodInvocation forEach = rewriter.getAST().newMethodInvocation();
					forEach.setName(rewriter.getAST().newSimpleName("forEach"));
					
					/* .forEach(<lambda>) */
					forEach.arguments().add(lambda);
					
					// Créer un methode parallel
					// l'invoquer sur stream
					final MethodInvocation parallel = rewriter.getAST().newMethodInvocation();
					parallel.setName(rewriter.getAST().newSimpleName("parallel"));
					parallel.setExpression(stream);
					
					/* stream().parallel().forEach() */
					forEach.setExpression(parallel);

					int circuitBreak = 10;
					
					while(circuitBreak-->0 && filterPhase(forEach));
					
					forEach.arguments().set(
						0,
						rethrowConsumer(ASTNode.copySubtree(ast, (ASTNode) forEach.arguments().get(0)))
					);
					
					rewriter.replace(enhancedForStatement, rewriter.getAST().newExpressionStatement(forEach), null);
				
			}
		});
	}
	
	@SuppressWarnings({ "unchecked", "restriction" })
	private boolean filterPhase(MethodInvocation forEach) {
		LambdaExpression lambdaExpForEach = (LambdaExpression)forEach.arguments().get(0);
		boolean isFilterOperation = false;
		Expression filterExpr = null;
		Statement filterStmt = null;
		if (lambdaExpForEach.getBody() instanceof Block)
		{
			Block block = (Block) lambdaExpForEach.getBody();
			if (block.statements().size() == 1)
			{
				if (block.statements().get(0) instanceof IfStatement)
				{
					IfStatement ifStmt = (IfStatement)block.statements().get(0);
					if (ifStmt.getElseStatement() == null)
					{
						isFilterOperation = true;
						filterExpr = ifStmt.getExpression();
						filterStmt = ifStmt.getThenStatement();
						
						lambdaExpForEach.setBody(ASTNodes.copySubtree(rewriter.getAST(), filterStmt));

						LambdaExpression lamdbaExprFilter = rewriter.getAST().newLambdaExpression();
						lamdbaExprFilter.parameters().add(ASTNodes.copySubtree(rewriter.getAST(), (ASTNode) ((LambdaExpression)forEach.arguments().get(0)).parameters().get(0)));
						lamdbaExprFilter.setBody(ASTNodes.copySubtree(rewriter.getAST(), filterExpr));
						
						MethodInvocation filter = rewriter.getAST().newMethodInvocation();
						filter.arguments().add(rethrowPredicate(lamdbaExprFilter));
						filter.setName(rewriter.getAST().newSimpleName("filter"));
						filter.setExpression((Expression) ASTNodes.copySubtree(rewriter.getAST(), forEach.getExpression()));
						
						forEach.setExpression(filter);
					}
				}
			}
		}
		
		return isFilterOperation;
	}
	
	@SuppressWarnings("unused")
	private MethodInvocation rethrowConsumer(ASTNode astNode) {
		return rethrow(astNode, "rethrowConsumer");
	}
	
	@SuppressWarnings("unused")
	private MethodInvocation rethrowPredicate(ASTNode astNode) {
		return rethrow(astNode, "rethrowPredicate");
	}
	
	private MethodInvocation rethrowFunction(ASTNode astNode) {
		return rethrow(astNode, "rethrowFunction");
	}	
	
	private MethodInvocation rethrow(ASTNode astNode, String methodName) {
		QualifiedName frlip6pjavaloopexploreutil = (QualifiedName) ast.newName("fr.lip6.pjava.loopexplore.util.LambdaExceptionUtil");
		ASTUtil.setAST(ast);
		return ASTUtil.newMethodInvocation(
			frlip6pjavaloopexploreutil,
			methodName,
			(Expression)astNode
		);
	}
}
