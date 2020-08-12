package fr.lip6.pjava.loopexplore.ui;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpOptionsInitializer;

public class CleanUpOptionsInitializer implements ICleanUpOptionsInitializer {

	@Override
	public void setDefaultOptions(CleanUpOptions options) {
		options.setOption("cleanup.archetype", CleanUpOptions.TRUE);
	}

}
