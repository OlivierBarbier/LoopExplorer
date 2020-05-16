package analyzer;

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
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;

public class EnhancedForLoopAnalyzer extends ASTVisitor
{
	private ITypeBinding rteBinding;
	private ITypeBinding cltnBinding;

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
	
	public EnhancedForLoopAnalyzer(EnhancedForStatement efs,ITypeBinding rteBinding,ITypeBinding cltnBinding)
	{
		this.efs = efs;
		this.rteBinding = rteBinding;
		this.cltnBinding = cltnBinding;
	}

	public void analyze()
	{
		efs.getBody().accept(this);
		
		analyzeNumberOfSuperForStatements();
	}
	
	private void analyzeNumberOfSuperForStatements()
	{
    	ASTNode node = efs.getParent();
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
		if ( ! efl.getExpression().resolveTypeBinding().isSubTypeCompatible(rteBinding))
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
				if ( ! exceptionType.isSubTypeCompatible(rteBinding)) {
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
			if ( ! nodeVariablebinding.getName().equals(efs.getParameter().getName().getIdentifier()))
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
		ITypeBinding tb = this.efs.getExpression().resolveTypeBinding();
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
		&&(getNumberOfSuperForStatements() == 0);
	}
	
	public String getFileName() {
		CompilationUnit root  = (CompilationUnit) efs.getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getElementName();
	}
	
	public String getPackageName() {
		CompilationUnit root  = (CompilationUnit) efs.getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getParent().getElementName();
	}
	
	public String getProjectName() {
		CompilationUnit root  = (CompilationUnit) efs.getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getJavaProject().getElementName();
	}

	public String getPath() {
		CompilationUnit root  = (CompilationUnit) efs.getRoot();
		ICompilationUnit javaSourceFile = (ICompilationUnit)root.getJavaElement();
		return javaSourceFile.getPath().toString();
	}
	
	public int getStartLine() {
		return ((CompilationUnit)efs.getRoot()).getLineNumber(efs.getStartPosition());
	}
	
	public int getEndLine() {
		return ((CompilationUnit)efs.getRoot()).getLineNumber(efs.getStartPosition()+efs.getLength());		
	}
	
	@SuppressWarnings("unchecked")
	public String getRefactoring() {
		/*if ( ! isRefactorable()) {
			return "Not Refactorable";
		}*/
		AST ast = this.efs.getAST();
	
		Expression expr = this.efs.getExpression();
		Document doc = new Document(efs.toString());
		MethodInvocation streams = ast.newMethodInvocation();
		
		ITypeBinding tb = expr.resolveTypeBinding(); 
		if (tb == null) {
			return "Not resolved type : " + expr.toString() ;
		} else if (tb.isArray())
		{
			/* java.util.Arrays(copy(expr))) */
			streams.setExpression(ast.newName(new String[]{"java", "util", "Arrays"}));
			streams.setName(ast.newSimpleName("stream"));
			streams.arguments().add(ASTNode.copySubtree(ast, expr));
		}
		else if (expr.resolveTypeBinding().isSubTypeCompatible(cltnBinding ))
		{
			/* expr.stream() */
			streams.setExpression((Expression) ASTNode.copySubtree(ast, expr));
			streams.setName(ast.newSimpleName("stream"));
				
		}
		
		MethodInvocation forEach = ast.newMethodInvocation();
		LambdaExpression lambdaExpForEach = ast.newLambdaExpression();
		lambdaExpForEach.parameters().add(ASTNode.copySubtree(ast, efs.getParameter()));	
		try {
			if (efs.getBody() instanceof Block) {
				lambdaExpForEach.setBody(ASTNode.copySubtree(ast, efs.getBody()));
			} else {
				Block block = ast.newBlock();
				block.statements().add(ASTNode.copySubtree(ast, efs.getBody()));
				lambdaExpForEach.setBody(block);
			}
		} catch (IllegalArgumentException ia) {
			System.err.println("Problem with setting " + efs.getBody() + " as body of lambda");
		}
		forEach.setExpression(streams);
		forEach.arguments().add(lambdaExpForEach);
		forEach.setName(ast.newSimpleName("forEach"));
		
		/* Next Phase */
		filterPhase(ast, forEach);
		mapPhase(ast, forEach);
		/**/
		doc = new Document(forEach.toString());

		return doc.get();
	}

	@SuppressWarnings("unchecked")
	private void filterPhase(AST ast, MethodInvocation forEach) {
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
			filter.arguments().add(lamdbaExprFilter);
			filter.setName(ast.newSimpleName("filter"));
			filter.setExpression((Expression) ASTNode.copySubtree(ast, forEach.getExpression()));
			
			forEach.setExpression(filter);
		}
	}

	@SuppressWarnings("unchecked")
	private void mapPhase(AST ast, MethodInvocation forEach) {
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
				map.arguments().add(mapLambdaExpr);
				map.setName(ast.newSimpleName("map"));
				

				SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
				sv.setName((SimpleName) ASTNode.copySubtree(ast, vdf.getName()));
				sv.setType((Type) ASTNode.copySubtree(ast, vds.getType()));
				lambdaExpForEach.parameters().set(0, sv);
				
				block.statements().remove(0);
				forEach.setExpression(map);
			}
		}
	}
}
