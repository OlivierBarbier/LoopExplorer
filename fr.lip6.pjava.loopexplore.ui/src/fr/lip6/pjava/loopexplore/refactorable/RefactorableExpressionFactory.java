package fr.lip6.pjava.loopexplore.refactorable;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class RefactorableExpressionFactory {

	public static IRefactorabeExpression make(Expression expr, ITypeBinding cltnBinding) {
		IRefactorabeExpression refactorableExpression;
		
		/*
		 * <expr> -> java.util.Arrays.stream(<expr>) 
		 */
		if (expr.resolveTypeBinding().isArray())
		{
			refactorableExpression = new RefactorableArray(expr);	
		}
		
		/*
		 * <expr> -> <expr>.stream() 
		 */		
		else if (expr.resolveTypeBinding().isSubTypeCompatible(cltnBinding))
		{
			refactorableExpression = new RefactorableCollection(expr);				
		}
		
		// ICI j'ai la garantie que la source de données est un Iterable
		// donc il est inutile de le vérifier de façon programmatique.
		// Comme je suis dans un boucle foreach, j'ai un Iterable ou un Array;
		// or je n'ai pas un array donc j'ai un Iterable.
		else 
		{
			refactorableExpression = new RefactorableIterable(expr);
		}
		
		return refactorableExpression;
	}

}
