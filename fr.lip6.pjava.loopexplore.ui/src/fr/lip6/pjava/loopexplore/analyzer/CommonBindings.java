package fr.lip6.pjava.loopexplore.analyzer;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class CommonBindings {
	private ITypeBinding rteBinding;
	private ITypeBinding cltnBinding;
	
	public CommonBindings(IJavaProject javaProject) throws JavaModelException {
		// Java 8 setting by default, but this setting is overruled by setProject below so irrelevant anyway.
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		// parse java files
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// actually this overrides a bunch of settings, adds the correct classpath etc... to the underlying ASTresolver/JDT compiler.
		parser.setProject(javaProject);
		// Important non default setting : resolve name/type bindings for us ! yes, please, thanks !
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		
		rteBinding = resolveITypeBindingFor("java.lang.RuntimeException",javaProject,parser);
		cltnBinding = resolveITypeBindingFor("java.util.Collection",javaProject,parser);
	}
	public ITypeBinding getRuntimeException() {
		return rteBinding;
	}
	public ITypeBinding getCollectionBinding() {
		return cltnBinding;
	}
	
	private ITypeBinding resolveITypeBindingFor(String qualifiedClassName, IJavaProject javaProject, ASTParser parser) throws JavaModelException {
		ITypeRoot itr = javaProject.findType(qualifiedClassName).getTypeRoot();
		parser.setSource(itr);
		parser.setResolveBindings(true);
		CompilationUnit node = (CompilationUnit)parser.createAST(null);
		return ((TypeDeclaration) node.types().get(0)).resolveBinding();
	}

}
