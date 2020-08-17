package fr.lip6.pjava.loopexplore.ui;

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
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import fr.lip6.pjava.loopexplore.analyzer.CommonBindings;
import fr.lip6.pjava.loopexplore.refactorable.IRefactorabeExpression;
import fr.lip6.pjava.loopexplore.refactorable.RefactorableExpressionFactory;
import fr.lip6.pjava.loopexplore.util.ASTUtil;
import fr.lip6.pjava.loopexplore.util.LambdaExceptionUtil;

@SuppressWarnings("restriction")
public class CleanUpFix implements ICleanUpFix {

	final private CompilationUnit compilationUnit;
	final private IJavaProject javaProject;

	final private ASTRewrite rewriter;
	
	final private CommonBindings cb;

	public CleanUpFix(CleanUpContext context) {
		compilationUnit = context.getAST();
		javaProject = compilationUnit.getJavaElement().getJavaProject();
		rewriter = ASTRewrite.create(compilationUnit.getAST());
		try {
			cb = new CommonBindings(javaProject);
		} catch (JavaModelException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		ICompilationUnit sourceDocument = (ICompilationUnit)compilationUnit.getJavaElement();
		final CompilationUnitChange compilationUnitChange = new CompilationUnitChange(
			sourceDocument.getElementName(), sourceDocument);

		int changes = rewriteRule02();
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

	int changed = 0;
	private int rewriteRule02() {
		changed = 0;
		compilationUnit.accept(new ASTVisitor() {
			@SuppressWarnings("unchecked")
			@Override
			public void endVisit(EnhancedForStatement enhancedForStatement) {
				IRefactorabeExpression refactorableExpression = 
						RefactorableExpressionFactory.make(enhancedForStatement.getExpression(), cb);
				
				final MethodInvocation stream = refactorableExpression.refactor();
				
		
				if (preconditions(enhancedForStatement)) {
					changed ++;
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
					setLambdaBody(lambda, enhancedForStatement.getBody());
					
					
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
					
					LambdaExpression le = ((LambdaExpression)forEach.arguments().get(0));
					
					Object parameter = le.parameters().get(0);
					if (parameter instanceof SingleVariableDeclaration) {
						SingleVariableDeclaration variable = (SingleVariableDeclaration)parameter;
						VariableDeclarationFragment fragment = getAst().newVariableDeclarationFragment();
						fragment.setName(ASTNodes.copySubtree(rewriter.getAST(), variable.getName()));
						parameter = fragment;
					}
					le.parameters().set(0, parameter);
					
					forEach.arguments().set(
						0,
						rethrowConsumer(ASTNode.copySubtree(getAst(), le))
					);
					
					rewriter.replace(enhancedForStatement, rewriter.getAST().newExpressionStatement(forEach), null);
				}
			}

			private boolean preconditions(EnhancedForStatement enhancedForStatement) {
				// TODO check preconditions
				return true;
			}
		});
		return changed;
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
						
						setLambdaBody(lambdaExpForEach, filterStmt);
						LambdaExpression lamdbaExprFilter = rewriter.getAST().newLambdaExpression();
						Object parameter = ((LambdaExpression)forEach.arguments().get(0)).parameters().get(0);
						if (parameter instanceof SingleVariableDeclaration) {
							SingleVariableDeclaration variable = (SingleVariableDeclaration)parameter;
							VariableDeclarationFragment fragment = getAst().newVariableDeclarationFragment();
							fragment.setName(ASTNodes.copySubtree(rewriter.getAST(), variable.getName()));
							parameter = fragment;
						}
						lamdbaExprFilter.parameters().add(ASTNodes.copySubtree(rewriter.getAST(), (ASTNode) parameter));
						lamdbaExprFilter.setBody(ASTNodes.copySubtree(rewriter.getAST(), filterExpr));
						
						MethodInvocation filter = rewriter.getAST().newMethodInvocation();
						filter.arguments().add(rethrowPredicate(lamdbaExprFilter));
						filter.setName(rewriter.getAST().newSimpleName("filter"));
						filter.setExpression(ASTNodes.copySubtree(rewriter.getAST(), forEach.getExpression()));
						
						forEach.setExpression(filter);
					}
				}
			}
		}
		
		return isFilterOperation;
	}

	private void setLambdaBody(LambdaExpression lambda, Statement body) {
		if (body instanceof Block) {
			lambda.setBody(ASTNodes.copySubtree(rewriter.getAST(), body));
		} else if (body instanceof ExpressionStatement){
			// ExpressionStatement
			lambda.setBody(ASTNodes.copySubtree(rewriter.getAST(), ((ExpressionStatement) body).getExpression()));								
		} else {
			// add a block
			Block block = (Block) rewriter.getAST().createInstance(ASTNode.BLOCK);
			block.statements().add(ASTNodes.copySubtree(rewriter.getAST(), body));
			lambda.setBody(block);											
		}
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
		QualifiedName frlip6pjavaloopexploreutil = (QualifiedName) getAst().newName("fr.lip6.pjava.loopexplore.util.LambdaExceptionUtil");
		ASTUtil.setAST(getAst());
		return ASTUtil.newMethodInvocation(
			frlip6pjavaloopexploreutil,
			methodName,
			(Expression)astNode
		);
	}

	private AST getAst() {
		return rewriter.getAST();
	}
}
