package collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.jdt.core.dom.ASTRequestor;
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

								// This version was supposed to be more efficient for parsing a group of files
								// but we can only ask for type resolution of elements within this set of files
								// So it will now work if your project has any external dependencies.
//								parser.createASTs(fragment.getCompilationUnits(), new String[0], new ASTRequestor() {
//									@Override
//									public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
//										parsedCu.add(ast);
//										super.acceptAST(source, ast);
//									}
//								}, new NullProgressMonitor());
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

	public void collectStreamVersion()
	{
		try {
			Arrays.stream(javaProject.getPackageFragmentRoots())
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
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}



	/**
	 * Go for a full parse+load into memory, with Binding resolution activated for the set of sources.
	 * This one pass parse ensures that IBindings resolve to a single instance in memory for a given object
	 * therefore we can lookup and store IBinding easily in the code (hash, equals, == all OK).
	 * 
	 * @param project input Java project carrying important classpath/JLS version etc...
	 * @param compilationUnits The set of input files we are considering as our scope.
	 * @param monitor a progress monitor, just forwarded to the parser.
	 * @return a set of fully resolved and parsed JDT DOM style document model for Java AST. We can use "node.resolveBinding()" in the resulting CompilationUnit.
	 */
	public List<CompilationUnit> parseSources(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) {

		// Java 8 setting by default, but this setting is overruled by setProject below so irrelevant anyway.
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		// parse java files
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// Important non default setting : resolve name/type bindings for us ! yes, please, thanks !
		parser.setResolveBindings(true);
		// actually this overrides a bunch of settings, adds the correct classpath etc... to the underlying ASTresolver/JDT compiler.
		parser.setProject(project);
		// Parse the set of input files in one big pass, collect results in parsedCu
		List<CompilationUnit> parsedCu = new ArrayList<>();
		parser.createASTs(compilationUnits, new String[0], new ASTRequestor() {
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				parsedCu.add(ast);
				super.acceptAST(source, ast);
			}
		}, monitor);
		return parsedCu;
	}


}
