package collector;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;

import analyzer.EnhancedForLoopAnalyzer;

public class EnhancedForLoopCollector extends ASTVisitor {
	private Collection<IJavaProject> javaProjectsCollection = new HashSet<>();
	public Collection<EnhancedForLoopAnalyzer> enhancedForStatementSet = new HashSet<>();

	public EnhancedForLoopCollector(IJavaProject[] javaProjects)
	{
		this.javaProjectsCollection.addAll(Arrays.asList(javaProjects));
	}
	
	public void collect()
	{
		javaProjectsCollection
			.parallelStream()
			
			.flatMap((IJavaProject javaProject) -> {
				try {
					return Arrays.stream(javaProject.getPackageFragmentRoots());
				} catch (JavaModelException e) { 
					// Trick to handle the checked exception
					throw new RuntimeException(e);
				}
			})
			
			.flatMap((IPackageFragmentRoot root) -> {
				try {
					return Arrays.stream(root.getChildren());
				} catch (JavaModelException e) {
					// Trick to handle the checked exception
					throw new RuntimeException(e);
				}
			})
			
			.filter((IJavaElement child) -> child.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
			
			.map((IJavaElement child) -> (IPackageFragment) child)
			
			.flatMap((IPackageFragment fragment) -> {
				try {
					return Arrays.stream(fragment.getCompilationUnits());
				} catch (JavaModelException e) {
					// Trick to handle the checked exception
					throw new RuntimeException(e);
				}
			})
			
			.map((ICompilationUnit icu) -> {
				ASTParser parser = ASTParser.newParser(AST.JLS13);
				parser.setSource(icu);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setResolveBindings(true);
				return (CompilationUnit) parser.createAST(null);
			})

			.forEach((CompilationUnit cu) -> cu.accept(this))
			;
	}
	
	public void endVisit(EnhancedForStatement efl)
	{
		
		EnhancedForLoopAnalyzer efla = new EnhancedForLoopAnalyzer(efl);
		efla.analyze();
		
		synchronized(this) {
			this.enhancedForStatementSet.add(efla);
		}
	}
	

}
