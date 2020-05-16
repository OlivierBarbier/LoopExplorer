package collector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import analyzer.EnhancedForLoopAnalyzer;

public class EnhancedForLoopCollector extends ASTVisitor {
	private IJavaProject javaProject ;
	public Collection<EnhancedForLoopAnalyzer> enhancedForStatementSet = ConcurrentHashMap.newKeySet();

	public EnhancedForLoopCollector(IJavaProject javaProjects)
	{
		this.javaProject = javaProjects;
	}

	public void collect() {

		// Java 8 setting by default, but this setting is overruled by setProject below so irrelevant anyway.
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		// parse java files
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// actually this overrides a bunch of settings, adds the correct classpath etc... to the underlying ASTresolver/JDT compiler.
		parser.setProject(javaProject);
		// Important non default setting : resolve name/type bindings for us ! yes, please, thanks !
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		
		try {
			// Parse the set of input files in one big pass, collect results in parsedCu
			List<CompilationUnit> parsedCu = new ArrayList<>();

			for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
				if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
					for (IJavaElement child :root.getChildren()) {
						if (child.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
							IPackageFragment fragment = (IPackageFragment) child;
							if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
								for (ICompilationUnit cu : fragment.getCompilationUnits()) {
									parser.setSource(cu);
									parser.setResolveBindings(true);
									
									CompilationUnit ccu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
									parsedCu.add(ccu);
								}
							}
						}
					}
				}
			}
			if (!parsedCu.isEmpty()) {
				ITypeBinding rteBinding = resolveITypeBindingFor("java.lang.RuntimeException",javaProject,parser,parsedCu.get(0));
				ITypeBinding cltnBinding = resolveITypeBindingFor("java.util.Collection",javaProject,parser,parsedCu.get(0));

				for (CompilationUnit cu : parsedCu) {
					cu.accept(new ASTVisitor() {
						@Override
						public void endVisit(EnhancedForStatement efl)
						{

							EnhancedForLoopAnalyzer efla = new EnhancedForLoopAnalyzer(efl,rteBinding,cltnBinding);
							efla.analyze();
							enhancedForStatementSet.add(efla);
						}

					});
				}
			}

		} catch (JavaModelException e) {
			throw new RuntimeException(e);
		}

	}


	private ITypeBinding resolveITypeBindingFor(String qualifiedClassName, IJavaProject javaProject, ASTParser parser, CompilationUnit cu) throws JavaModelException {
		ITypeBinding res = cu.getAST().resolveWellKnownType(qualifiedClassName);
		if (res != null) {
			return res;
		}
		ITypeRoot itr = javaProject.findType(qualifiedClassName).getTypeRoot();
		parser.setSource(itr);
		parser.setResolveBindings(true);
		CompilationUnit node = (CompilationUnit)parser.createAST(new NullProgressMonitor());
		return ((TypeDeclaration) node.types().get(0)).resolveBinding();
	}

}
