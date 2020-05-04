package analyzer;

import java.util.Arrays;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class EnhancedForLoopAnalyzer extends ASTVisitor
{
	private static ITypeBinding rteBinding;

	private static ITypeBinding cltnBinding;

	public EnhancedForStatement efs;
	
	int children = -1;
	int parent   = +0;
	int brk      = +0;
	int cntn     = +0;
	int thrw     = +0;
	int rtrn     = +0;
	// Neither Final Nor Effectively Final
    int nfnef    = +0;

	int yf       = +0;
	
	public EnhancedForLoopAnalyzer(EnhancedForStatement efs)
	{
		this.efs = efs;
	}

	public void analyze()
	{
		if (EnhancedForLoopAnalyzer.rteBinding == null)
		{
			EnhancedForLoopAnalyzer.rteBinding = resolveITypeBindingFor("java.lang.RuntimeException");
			EnhancedForLoopAnalyzer.cltnBinding = resolveITypeBindingFor("java.util.Collection");
		}
		efs.accept(this);
		
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
	
	public void endVisit(EnhancedForStatement efl)
	{
		children++;
	}	
	
	public void endVisit(ForStatement efl)
	{
		children++;
	}

	public void endVisit(BreakStatement efl)
	{
		brk++;
	}

	public void endVisit(ContinueStatement efl)
	{
		cntn++;
	}

	public void endVisit(ThrowStatement efl)
	{
		if ( ! efl.getExpression().resolveTypeBinding().isSubTypeCompatible(EnhancedForLoopAnalyzer.rteBinding))
		{
			thrw++;
		}
	}
	
	public void endVisit(MethodInvocation mi)
	{	
		for(ITypeBinding exceptionType:mi.resolveMethodBinding().getExceptionTypes())
		{
			if ( ! exceptionType.isSubTypeCompatible(EnhancedForLoopAnalyzer.rteBinding)) {
				thrw++;
			}
		}
	}
	
	protected ITypeBinding resolveITypeBindingFor(String qualifiedClassName)
	{
		try {
			// https://stackoverflow.com/questions/25834846/resolve-bindings-for-new-created-types
			// https://stackoverflow.com/questions/25916505/how-to-get-an-itypebinding-from-a-class-or-interface-name-string-with-eclipse			
			ASTParser parser = ASTParser.newParser(AST.JLS13);
			ITypeRoot itr = ((CompilationUnit)efs.getRoot()).getJavaElement().getJavaProject().findType(qualifiedClassName).getTypeRoot();
			parser.setSource(itr);
			parser.setResolveBindings(true);
			CompilationUnit node = (CompilationUnit)parser.createAST(new NullProgressMonitor());
			return ((TypeDeclaration) node.types().get(0)).resolveBinding();
		} catch (JavaModelException e1) {
			throw new RuntimeException("Cannot Parse "+qualifiedClassName);
		}
	}
	
	public void endVisit(ReturnStatement efl)
	{
		rtrn++;
	}

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
		return this.efs.getExpression().resolveTypeBinding().getQualifiedName();
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
		return false;
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
		
		if (expr.resolveTypeBinding().isArray())
		{
			/* java.util.Arrays(copy(expr))) */
			streams.setExpression(ast.newName(new String[]{"java", "util", "Arrays"}));
			streams.setName(ast.newSimpleName("stream"));
			streams.arguments().add(ASTNode.copySubtree(ast, expr));
		}
		else if (expr.resolveTypeBinding().isSubTypeCompatible(EnhancedForLoopAnalyzer.cltnBinding ))
		{
			/* expr.stream() */
			streams.setExpression((Expression) ASTNode.copySubtree(ast, expr));
			streams.setName(ast.newSimpleName("stream"));
				
		}
		
		MethodInvocation forEach = ast.newMethodInvocation();
		LambdaExpression lambdaExpForEach = ast.newLambdaExpression();
		lambdaExpForEach.parameters().add(ASTNode.copySubtree(ast, efs.getParameter()));	
		lambdaExpForEach.setBody(ASTNode.copySubtree(ast, efs.getBody()));
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
			if (block.statements().get(0) instanceof VariableDeclarationStatement)
			{
				MethodInvocation map = ast.newMethodInvocation();
				map.setExpression((Expression) ASTNode.copySubtree(ast, forEach.getExpression()));
				LambdaExpression mapLambdaExpr = ast.newLambdaExpression();
				
				VariableDeclarationStatement vds = (VariableDeclarationStatement)block.statements().get(0);
				VariableDeclarationFragment vdf = (VariableDeclarationFragment)vds.fragments().get(0);
				
				/* retirer le premier statement */
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
