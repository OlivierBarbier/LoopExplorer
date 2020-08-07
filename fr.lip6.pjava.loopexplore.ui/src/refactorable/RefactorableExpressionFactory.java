package refactorable;

import org.eclipse.jdt.core.dom.Expression;

import analyzer.EnhancedForLoopAnalyzer;

public class RefactorableExpressionFactory {

	public static IRefactorabeExpression make(Expression expr) {
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
		else if (expr.resolveTypeBinding().isSubTypeCompatible(EnhancedForLoopAnalyzer.cltnBinding))
		{
			refactorableExpression = new RefactorableCollection(expr);				
		}
		
		// ICI j'ai la garantie que la source de données est un Iterable
		// dnc il est inutile de le vérifier de façon programmatique.
		// comme je suis dans un boucle foreach j'ai un Iterable ou un Array
		// or je n'ai pas un array donc j'ai un Iterable (merci les cours de logique !)
		else 
		{
			refactorableExpression = new RefactorableIterable(expr);
		}
		
		return refactorableExpression;
	}

}
