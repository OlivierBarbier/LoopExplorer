package handler;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import fr.lip6.pjava.loopexplore.Refactor;



public class CommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		/* <CUSTOM CODE HERE> */
		ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);
		List<?> list = SelectionUtil.toList(selection);
		List<IJavaProject> javaProjects = list.stream()
				.filter(e -> e instanceof IJavaProject)
				.map(e -> (IJavaProject) e)
				.collect(Collectors.toList());

		Refactor.refactorProjects(javaProjects);

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		MessageDialog.openInformation(
				window.getShell(),
				"Handler",
				"Hello, Eclipse world");
		return null;
	}

}
