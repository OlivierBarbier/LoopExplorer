package fr.lip6.pjava.loopexplore.refactorable;

import org.eclipse.jdt.core.dom.MethodInvocation;

public interface IRefactorabeExpression {

	MethodInvocation refactor();

}