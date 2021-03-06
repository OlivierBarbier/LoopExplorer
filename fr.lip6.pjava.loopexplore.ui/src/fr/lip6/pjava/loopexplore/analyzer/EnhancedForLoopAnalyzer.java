package fr.lip6.pjava.loopexplore.analyzer;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;

import fr.lip6.pjava.loopexplore.refactorable.IRefactorabeExpression;
import fr.lip6.pjava.loopexplore.refactorable.RefactorableExpressionFactory;
import fr.lip6.pjava.loopexplore.util.ASTUtil;

public class EnhancedForLoopAnalyzer extends ASTVisitor
{
	private CommonBindings cb;
	public EnhancedForStatement efs;
	
	int children = +0;
	int parent   = +0;
	int brk      = +0;
	int cntn     = +0;
	int thrw     = +0;
	int rtrn     = +0;
	// Neither Final Nor Effectively Final
    int nfnef    = +0;

	int yf       = +0;
	
	public EnhancedForLoopAnalyzer(EnhancedForStatement efs, CommonBindings cb)
	{
		this.efs = efs;
		this.cb = cb;
	}

	public void analyze()
	{
		getEFS().getBody().accept(this);
		
		analyzeNumberOfSuperForStatements();
	}
	
	private void analyzeNumberOfSuperForStatements()
	{
    	ASTNode node = getEFS().getParent();
    	while(node != null)
    	{
    		if (node instanceof EnhancedForStatement || node instanceof ForStatement) {
    			parent++;
    		}
    		node = node.getParent();
    	}		
	}
	
	@Override
	public void endVisit(EnhancedForStatement efl)
	{
		children++;
	}	
	
	@Override
	public void endVisit(ForStatement efl)
	{
		children++;
	}

	@Override
	public void endVisit(BreakStatement efl)
	{
		brk++;
	}

	@Override
	public void endVisit(ContinueStatement efl)
	{
		cntn++;
	}

	@Override
	public void endVisit(ThrowStatement efl)
	{
		
		ITypeBinding resolveTypeBinding = efl.getExpression().resolveTypeBinding();
		if ( resolveTypeBinding == null || ! resolveTypeBinding.isSubTypeCompatible(cb.getRuntimeException()))
		{
			thrw++;
		}
	}
	
	@Override
	public void endVisit(MethodInvocation mi)
	{	
		IMethodBinding rmb = mi.resolveMethodBinding();
		if (rmb == null) {
			thrw++;
		} else {
			for(ITypeBinding exceptionType:rmb.getExceptionTypes())
			{
				if ( ! exceptionType.isSubTypeCompatible(cb.getRuntimeException())) {
					thrw++;
				}
			}
		}
	}
	
	@Override
	public void endVisit(ReturnStatement efl)
	{
		rtrn++;
	}

	@Override
	public void endVisit(SimpleName node) {
		IBinding nodeBinding = node.resolveBinding();
		if (node.resolveBinding() instanceof org.eclipse.jdt.core.dom.IVariableBinding) {
			IVariableBinding nodeVariablebinding = (IVariableBinding)nodeBinding;
			if ( ! nodeVariablebinding.getName().equals(getEFS().getParameter().getName().getIdentifier()))
			{
				int flags = nodeVariablebinding.getModifiers();
				
				if (!org.eclipse.jdt.core.dom.Modifier.isFinal(flags) && !nodeVariablebinding.isEffectivelyFinal()) {
					nfnef++;
				}
			}
		}
	}
	
	public int getNumberOfSubForStatements()
	{
		return children;
	}
	
	public int getNumberOfSuperForStatements()
	{
		return parent;
	}
	
	public int getNumberOfBreakStatements()
	{
		return brk;
	}
	
	public int getNumberOfContinueStatements()
	{
		return cntn;
	}
	
	public int getNumberOfReturnStatements()
	{
		return rtrn;
	}

	public int getNumberOfNeitherFinalNorEffectivelyFinalVariables()
	{
		return nfnef;
	}

	public int getNumberOfThrowStatements()
	{
		return thrw;
	}	
	
	public String getIterableClassName()
	{
		ITypeBinding tb = this.getEFS().getExpression().resolveTypeBinding();
		if (tb != null)
			return tb.getQualifiedName();
		else 
			return "java.lang.Iterable";
	}
	
	public boolean isIteratingOverACollection()
	{
		return false;
	}
	
	public boolean bodyDoesNotTriggerCheckedExceptions()
	{
		return false;
	}
	
	public boolean bodyDoesNotContainNeitherNonEffectivelyFinalNorNonFinalVariables()
	{
		return false;
	}
	
	public boolean bodyDoesNotContainBreakStatements()
	{
		return brk == 0;
	}
	
	public boolean bodyDoesNotContainReturnStatements()
	{
		return rtrn == 0;
	}
	
	public boolean bodyDoesNotContainContinueStatements()
	{
		return cntn == 0;
	}
	
	public boolean isRefactorable()
	{
		return (getNumberOfBreakStatements() == 0)
		&& (getNumberOfContinueStatements() == 0)
		&& (getNumberOfNeitherFinalNorEffectivelyFinalVariables() == 0)
		&& (getNumberOfReturnStatements() == 0)
		&& (getNumberOfSubForStatements() == 0)
		&& (getNumberOfSuperForStatements() == 0)
		//&& (getNumberOfThrowStatements() > 0)
		;
	}
	
	public String getFileName() {
		CompilationUnit root  = (CompilationUnit) getEFS().getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getElementName();
	}
	
	public String getPackageName() {
		CompilationUnit root  = (CompilationUnit) getEFS().getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getParent().getElementName();
	}
	
	public String getProjectName() {
		CompilationUnit root  = (CompilationUnit) getEFS().getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getJavaProject().getElementName();
	}

	public String getPath() {
		CompilationUnit root  = (CompilationUnit) getEFS().getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getPath().toString();
	}
	
	public int getStartLine() {
		return ((CompilationUnit)getEFS().getRoot()).getLineNumber(getEFS().getStartPosition());
	}
	
	public int getEndLine() {
		return ((CompilationUnit)getEFS().getRoot()).getLineNumber(getEFS().getStartPosition()+getEFS().getLength());		
	}
	
	@SuppressWarnings("unchecked")
	public String getRefactoring() {
		/*if ( ! isRefactorable()) {
			return "Not Refactorable";
		}*/
		AST ast = this.getEFS().getAST();
		
		ASTUtil.setAST(ast);

		IRefactorabeExpression refactorableExpression = 
				RefactorableExpressionFactory.make(getEFS().getExpression(),cb);
					
		/* <refactorableExpression.refactor()>.forEach(fr.lip6.pjava.loopexplore.util.rethrowConsumer(<lambdaExprForEach>)) */
		MethodInvocation forEach = ASTUtil.newMethodInvocation(
			refactorableExpression.refactor(), 
			"forEach", 
				ASTUtil.newLambdaExpression(
					getEFS().getParameter(),
					getEFS().getBody()
				)
		);
		
		/* Next Phase */
		
		boolean filtered = false;
		boolean mapped = false;
		
		int circuitBreaker = 10;
		do {
			filtered = filterPhase(ast, forEach);
			// mapped = mapPhase(ast, forEach);
			circuitBreaker--;
			if (circuitBreaker == 0) {
				throw new RuntimeException("Circuit breaker triggered!");
			}
		} while(filtered || mapped);
		
		forEach
			.arguments()
			.set(
				0,
				this.rethrowConsumer(ast, (ASTNode)forEach.arguments().get(0))
			)
		;

		Document doc = new Document(ast.newExpressionStatement(forEach).toString());

		return doc.get();
	}

	private MethodInvocation rethrowConsumer(AST ast, ASTNode astNode) {
		return rethrow(ast, astNode, "rethrowConsumer");
	}

	private MethodInvocation rethrowFunction(AST ast, ASTNode astNode) {
		return rethrow(ast, astNode, "rethrowFunction");
	}	
	
	private MethodInvocation rethrow(AST ast, ASTNode astNode, String methodName) {
		QualifiedName frlip6pjavaloopexploreutil = 
			ast.newQualifiedName(
				ast.newQualifiedName(
					ast.newQualifiedName(
						ast.newQualifiedName(
							ast.newQualifiedName(
								ast.newSimpleName("fr"),
								ast.newSimpleName("lip6")
							),
						ast.newSimpleName("pjava")
					),
					ast.newSimpleName("loopexplore")
				),
				ast.newSimpleName("util")
			),
			ast.newSimpleName("LambdaExceptionUtil")
		);
		
		return ASTUtil.newMethodInvocation(
			frlip6pjavaloopexploreutil,
			methodName,
			(Expression)ASTNode.copySubtree(ast, astNode)
		);
	}	
	
	@SuppressWarnings("unchecked")
	private boolean filterPhase(AST ast, MethodInvocation forEach) {
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
					}
				}
			}
		}
		
		if (isFilterOperation)
		{
			lambdaExpForEach.setBody(ASTNode.copySubtree(ast, filterStmt));

			LambdaExpression lamdbaExprFilter = ast.newLambdaExpression();
			lamdbaExprFilter.parameters().add(ASTNode.copySubtree(ast, (ASTNode) ((LambdaExpression)forEach.arguments().get(0)).parameters().get(0)));
			lamdbaExprFilter.setBody(ASTNode.copySubtree(ast, filterExpr));
			
			MethodInvocation filter = ast.newMethodInvocation();
			filter.arguments().add(this.rethrowFunction(ast, lamdbaExprFilter));
			filter.setName(ast.newSimpleName("filter"));
			filter.setExpression((Expression) ASTNode.copySubtree(ast, forEach.getExpression()));
			
			forEach.setExpression(filter);
		}
		
		return isFilterOperation;
	}

	@SuppressWarnings("unchecked")
	private boolean mapPhase(AST ast, MethodInvocation forEach) {
		LambdaExpression lambdaExpForEach = (LambdaExpression)forEach.arguments().get(0);
		boolean isMapOperation = false;

		if (lambdaExpForEach.getBody() instanceof Block)
		{
			Block block = (Block) lambdaExpForEach.getBody();
			if (! block.statements().isEmpty() && block.statements().get(0) instanceof VariableDeclarationStatement)
			{
				MethodInvocation map = ast.newMethodInvocation();
				map.setExpression((Expression) ASTNode.copySubtree(ast, forEach.getExpression()));
				LambdaExpression mapLambdaExpr = ast.newLambdaExpression();
				
				VariableDeclarationStatement vds = (VariableDeclarationStatement)block.statements().get(0);
				VariableDeclarationFragment vdf = (VariableDeclarationFragment)vds.fragments().get(0);
				
				mapLambdaExpr.setBody(ASTNode.copySubtree(ast, vdf.getInitializer()));
				mapLambdaExpr.parameters().add(ASTNode.copySubtree(ast, (ASTNode) lambdaExpForEach.parameters().get(0)));
				map.arguments().add(this.rethrowFunction(ast, mapLambdaExpr));
				map.setName(ast.newSimpleName("map"));
				

				SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
				sv.setName((SimpleName) ASTNode.copySubtree(ast, vdf.getName()));
				sv.setType((Type) ASTNode.copySubtree(ast, vds.getType()));
				lambdaExpForEach.parameters().set(0, sv);
				
				block.statements().remove(0);
				forEach.setExpression(map);
				isMapOperation = true;
			}
		}
		
		return isMapOperation;
	}

	public EnhancedForStatement getEFS() {
		return efs;
	}
}
