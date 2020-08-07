package refactorable;

import org.eclipse.jdt.core.dom.MethodInvocation;

public interface IRefactorabeExpression {

	MethodInvocation refactor();

}