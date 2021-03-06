package jadex.micro.testcases;

import java.util.Collection;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IArgumentsResultsFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

/**
 *  Test searching for services that don't exist. 
 */
@Description("Test searching for services that don't exist.")
@Results(@Result(name="testresults", clazz=Testcase.class))
@Agent
public class NoServiceAgent
{
	@Agent
	protected IInternalAccess agent;
	
	@AgentBody
	public IFuture<Void> executeBody()
	{
		final Future<Void> ret = new Future<Void>();
		
		final TestReport	tr	= new TestReport("#1", "Searching for services.");
		
		agent.getComponentFeature(IRequiredServicesFeature.class).searchServices(INoService.class).addResultListener(new IResultListener<Collection<INoService>>()
		{
			public void resultAvailable(Collection<INoService> result)
			{
				if(result.isEmpty())
				{
					tr.setSucceeded(true);
				}
				else
				{
					tr.setFailed("Expected empty collection but was: "+result);
				}
				agent.getComponentFeature(IArgumentsResultsFeature.class).getResults().put("testresults", new Testcase(1, new TestReport[]{tr}));
				ret.setResult(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				tr.setFailed("Exception during test: "+exception);
				agent.getComponentFeature(IArgumentsResultsFeature.class).getResults().put("testresults", new Testcase(1, new TestReport[]{tr}));
				ret.setResult(null);
			}
		});
		
		return ret;
	}
	
	/** Test service interface. */
	public static interface	INoService	extends IService {}
}
