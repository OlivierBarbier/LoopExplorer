package fr.lip6.pjava.loopexplore.ui;

import java.io.IOException;
import java.util.List;

import fr.lip6.move.gal.Specification;
import fr.lip6.pjava.loopexplore.orders.OrderBuilder;
import fr.lip6.pjava.loopexplore.ui.handlers.OrderHandler;

public class AllVariables extends OrderHandler {


	@Override
	protected String getServiceName() {		
		return "Contribution";
	}

	@Override
	public void workOnSpec(Specification s, String outpath) throws IOException {
		OrderBuilder ob = new OrderBuilder();
		List<String> order = ob.buildOrder(s);
		ob.printOrder(outpath, order);
	}

}
