package jadex.micro;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import jadex.bridge.BasicComponentIdentifier;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.component.IComponentFeatureFactory;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.factory.IComponentFactory;
import jadex.bridge.service.types.factory.SComponentFactory;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.library.ILibraryServiceListener;
import jadex.commons.LazyResource;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.kernelbase.IBootstrapFactory;
import jadex.micro.features.impl.MicroInjectionComponentFeature;
import jadex.micro.features.impl.MicroLifecycleComponentFeature;
import jadex.micro.features.impl.MicroMessageComponentFeature;
import jadex.micro.features.impl.MicroPojoComponentFeature;
import jadex.micro.features.impl.MicroServiceInjectionComponentFeature;


/**
 *  Factory for creating micro agents.
 */
public class MicroAgentFactory extends BasicService implements IComponentFactory, IBootstrapFactory
{
	//-------- constants --------
	
	/** The supported component types (file extensions).
	 *  Convention used by platform config panel. */
	public static final String[]	FILETYPES	= new String[]{"Agent.class"};
	
	/** The micro agent file type. */
	public static final String	FILETYPE_MICROAGENT	= "Micro Agent";
	
	/** The image icon. */
	protected static final LazyResource ICON = new LazyResource(MicroAgentFactory.class, "/jadex/micro/images/micro_agent.png");
	
	/** The specific component features for micro agents. */
	public static final Collection<IComponentFeatureFactory>	MICRO_FEATURES	= Collections.unmodifiableCollection(
		Arrays.asList(
			MicroPojoComponentFeature.FACTORY,
			MicroInjectionComponentFeature.FACTORY,
			MicroServiceInjectionComponentFeature.FACTORY,
			MicroLifecycleComponentFeature.FACTORY,
			MicroMessageComponentFeature.FACTORY
		));

	//-------- attributes --------
	
	/** The application model loader. */
	protected MicroModelLoader loader;
	
	/** The platform. */
	protected IInternalAccess provider;
	
	/** The properties. */
	protected Map<String, Object> fproperties;
	
//	/** The library service. */
//	protected ILibraryService libservice;
	
	/** The library service listener */
	protected ILibraryServiceListener libservicelistener;
	
	/** The standard + micro component features. */
	protected Collection<IComponentFeatureFactory>	features;
	
	//-------- constructors --------
	
	/**
	 *  Create a new agent factory.
	 */
	public MicroAgentFactory(IInternalAccess provider, Map<String, Object> properties)
	{
		super(provider.getComponentIdentifier(), IComponentFactory.class, null);

		this.provider = provider;
		this.fproperties = properties;
		this.loader = new MicroModelLoader();
		
		this.libservicelistener = new ILibraryServiceListener()
		{
			public IFuture<Void> resourceIdentifierRemoved(IResourceIdentifier parid, IResourceIdentifier rid)
			{
				loader.clearModelCache();
				return IFuture.DONE;
			}
			
			public IFuture<Void> resourceIdentifierAdded(IResourceIdentifier parid, IResourceIdentifier rid, boolean rem)
			{
				loader.clearModelCache();
				return IFuture.DONE;
			}
		};
		ILibraryService ls = getLibraryService();
		if(ls!=null)
		{
//			System.out.println("library listener on: "+this);
			ls.addLibraryServiceListener(libservicelistener);
		}
//		else
//		{
//			System.out.println("no library listener on: "+this);
//		}
		
		features	= SComponentFactory.orderComponentFeatures(SReflect.getUnqualifiedClassName(getClass()), Arrays.asList(SComponentFactory.DEFAULT_FEATURES, MICRO_FEATURES));
	}
	
	/**
	 *  Create a new agent factory for startup.
	 *  @param platform	The platform.
	 */
	// This constructor is used by the Starter class and the ADFChecker plugin. 
	public MicroAgentFactory(String providerid)
	{
		super(new BasicComponentIdentifier(providerid), IComponentFactory.class, null);
		this.loader = new MicroModelLoader();
		features	= SComponentFactory.orderComponentFeatures(SReflect.getUnqualifiedClassName(getClass()), Arrays.asList(SComponentFactory.DEFAULT_FEATURES, MICRO_FEATURES));
	}
	
	/**
	 *  Start the service.
	 */
	public IFuture<Void> startService(IInternalAccess component, IResourceIdentifier rid)
	{
		this.provider = component;
		this.providerid = provider.getComponentIdentifier();
		createServiceIdentifier("BootstrapFactory", IComponentFactory.class, rid, IComponentFactory.class, null);
		return startService();
	}
	
//	/**
//	 *  Start the service.
//	 */
//	public IFuture<Void> startService()
//	{
//		final Future<Void> ret = new Future<Void>();
//		SServiceProvider.getService(provider, ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM)
//			.addResultListener(new ExceptionDelegationResultListener<ILibraryService, Void>(ret)
//		{
//			public void customResultAvailable(ILibraryService result)
//			{
//				libservice = result;
//				libservice.addLibraryServiceListener(libservicelistener);
//				MicroAgentFactory.super.startService()
//					.addResultListener(new DelegationResultListener<Void>(ret));
//			}
//		});
//		return ret;
//	}
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	public synchronized IFuture<Void>	shutdownService()
	{
		final Future<Void>	ret	= new Future<Void>();
		if(libservicelistener!=null)
		{
			getLibraryService().removeLibraryServiceListener(libservicelistener)
				.addResultListener(new DelegationResultListener<Void>(ret)
			{
				public void customResultAvailable(Void result)
				{
					MicroAgentFactory.super.shutdownService()
						.addResultListener(new DelegationResultListener<Void>(ret));
				}
			});
		}
			
		return ret;
	}
	
	//-------- IAgentFactory interface --------
	
	/**
	 *  Load a  model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return The loaded model.
	 */
	public IFuture<IModelInfo> loadModel(final String model, final String[] imports, final IResourceIdentifier rid)
	{
		final Future<IModelInfo> ret = new Future<IModelInfo>();
//		System.out.println("filename: "+filename);
		
//		if(model.indexOf("HelloWorld")!=-1)
//			System.out.println("hw");
		
		try
		{
			MicroModel	cached	= (MicroModel)loader.getCachedModel(model, MicroModelLoader.FILE_EXTENSION_MICRO, imports, rid);
			if(cached!=null)
			{
				ret.setResult(cached.getModelInfo());
			}
			else
			{
				ILibraryService libservice = getLibraryService();
				if(libservice!=null)
				{
					libservice.getClassLoader(rid)
						.addResultListener(new ExceptionDelegationResultListener<ClassLoader, IModelInfo>(ret)
					{
						public void customResultAvailable(ClassLoader cl)
						{
							try
							{
								IModelInfo mi = loader.loadComponentModel(model, imports, rid, cl, new Object[]{rid, getProviderId().getRoot(), features}).getModelInfo();
								ret.setResult(mi);
							}
							catch(Exception e)
							{
								ret.setException(e);
							}
						}
					});		
				}
				else
				{
					try
					{
						ClassLoader cl = getClass().getClassLoader();
						IModelInfo mi = loader.loadComponentModel(model, imports, rid, cl, new Object[]{rid, getProviderId().getRoot(), features}).getModelInfo();
						ret.setResult(mi);
					}
					catch(Exception e)
					{
						ret.setException(e);
					}			
				}
			}
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Test if a model can be loaded by the factory.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if model can be loaded.
	 */
	public IFuture<Boolean> isLoadable(String model, String[] imports, IResourceIdentifier rid)
	{
//		System.out.println("isLoadable (micro): "+model);
		
		boolean ret = model.toLowerCase().endsWith("agent.class");
//		if(model.toLowerCase().endsWith("Agent.class"))
//		{
//			ILibraryService libservice = (ILibraryService)platform.getService(ILibraryService.class);
//			String clname = model.substring(0, model.indexOf(".class"));
//			Class cma = SReflect.findClass0(clname, null, libservice.getClassLoader());
//			ret = cma!=null && cma.isAssignableFrom(IMicroAgent.class);
//			System.out.println(clname+" "+cma+" "+ret);
//		}
		return ret ? IFuture.TRUE : IFuture.FALSE;
	}
	
	/**
	 *  Test if a model is startable (e.g. an component).
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return True, if startable (and loadable).
	 */
	public IFuture<Boolean> isStartable(final String model, final String[] imports, final IResourceIdentifier rid)
	{
		IFuture<Boolean>	ret;
		
		if(isLoadable(model, imports, rid).get().booleanValue())
		{
			final Future<Boolean>	fut	= new Future<Boolean>();
			ret	= fut;
			loadModel(model, imports, rid).addResultListener(new IResultListener<IModelInfo>()
			{
				public void resultAvailable(final IModelInfo mi) 
				{
					ILibraryService libservice = getLibraryService();
					if(libservice!=null)
					{
						libservice.getClassLoader(rid)
							.addResultListener(new ExceptionDelegationResultListener<ClassLoader, Boolean>(fut)
						{
							public void customResultAvailable(ClassLoader cl)
							{
								try
								{
									Class<?> clazz = getMicroAgentClass(mi.getFullName()+"Agent", null, cl);
									fut.setResult(!Modifier.isInterface(clazz.getModifiers()) && !Modifier.isAbstract(clazz.getModifiers()));
								}
								catch(Exception e)
								{
									fut.setResult(Boolean.FALSE);
//											ret.setException(e);
								}
							}
						});		
					}
					else
					{
						try
						{
							ClassLoader cl = getClass().getClassLoader();
							Class<?> clazz = getMicroAgentClass(mi.getFullName()+"Agent", null, cl);
							fut.setResult(!Modifier.isAbstract(clazz.getModifiers()));
						}
						catch(Exception e)
						{
							fut.setResult(Boolean.FALSE);
//									ret.setException(e);
						}
					}
				}
				
				public void exceptionOccurred(Exception exception)
				{
//							exception.printStackTrace();
					Logger.getLogger(MicroAgentFactory.class.toString()).warning(exception.toString());
					fut.setResult(Boolean.FALSE);
//							super.exceptionOccurred(exception);
				}
			});
		}
		else
		{
			ret	= IFuture.FALSE;
		}
		
		return ret;
	}

	/**
	 *  Get the names of ADF file types supported by this factory.
	 */
	public String[] getComponentTypes()
	{
		return new String[]{FILETYPE_MICROAGENT};
	}

	/**
	 *  Get a default icon for a file type.
	 */
	public IFuture<byte[]> getComponentTypeIcon(String type)
	{		
		Future<byte[]>	ret	= new Future<byte[]>();
		if(type.equals(FILETYPE_MICROAGENT))
		{
			try
			{
				ret.setResult(ICON.getData());
//				System.out.println("icon for micro: "+ICON.getData().length);
			}
			catch(IOException e)
			{
				e.printStackTrace();
				ret.setException(e);
			}
		}
		else
		{
//			System.out.println("not found: "+type);
			ret.setResult(null);
		}
		
		
		return ret;
	}	

	/**
	 *  Get the component type of a model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 */
	public IFuture<String> getComponentType(String model, String[] imports, IResourceIdentifier rid)
	{
		return new Future<String>(model.toLowerCase().endsWith("agent.class") ? FILETYPE_MICROAGENT: null);
	}
	
	/**
	 *  Get the component features for a model.
	 *  @param model The component model.
	 *  @return The component features.
	 */
	public IFuture<Collection<IComponentFeatureFactory>> getComponentFeatures(IModelInfo model)
	{
//		Collection<IComponentFeatureFactory> ret = features;
//		if(model.getFeatures().length>0)
//			ret = SUtil.arrayToSet(model.getFeatures());
		return new Future<Collection<IComponentFeatureFactory>>(model.getFeatures().length==0? features: (Collection)SUtil.arrayToList(model.getFeatures()));
	}

//	/**
//	 * Create a component interpreter.
//	 * @param model The component model.
//	 * @param component The platform component.
//	 * @param persistinfo The previously saved interpreter-specific state (if any).
//	 * @return An interpreter for the component.
//	 */
//	public IFuture<IComponentInterpreter> createComponentInterpreter(final IModelInfo model, final IInternalAccess component, final Object persistinfo)
//	{
//		final Future<IComponentInterpreter> res = new Future<IComponentInterpreter>();
//		
//		if(libservice!=null)
//		{
//			libservice.getClassLoader(model.getResourceIdentifier())
//				.addResultListener(new ExceptionDelegationResultListener<ClassLoader, IComponentInterpreter>(res)
//			{
//				public void customResultAvailable(ClassLoader cl)
//				{
//					try
//					{
//						MicroModel mm = loader.loadComponentModel(model.getFilename(), null, cl, new Object[]{model.getResourceIdentifier(), getProviderId().getRoot()});
//						MicroAgentInterpreter mai = new MicroAgentInterpreter(mm, getMicroAgentClass(model.getFullName()+"Agent", null, cl), component, (MicroAgentPersistInfo)persistinfo);
//						res.setResult(mai);
//					}
//					catch(Exception e)
//					{
//						res.setException(e);
//					}
//				}
//			});
//		}
//		
//		// For platform bootstrapping
//		else
//		{
//			try
//			{
//				ClassLoader	cl	= getClass().getClassLoader();
//				MicroModel mm = loader.loadComponentModel(model.getFilename(), null, cl, new Object[]{model.getResourceIdentifier(), getProviderId().getRoot()});
//				MicroAgentInterpreter mai = new MicroAgentInterpreter(mm, getMicroAgentClass(model.getFullName()+"Agent", null, cl), component, (MicroAgentPersistInfo)persistinfo);
//				res.setResult(mai);
//			}
//			catch(Exception e)
//			{
//				res.setException(e);
//			}
//		}
//
//		return res;
////		return new Future<Tuple2<IComponentInstance, IComponentAdapter>>(new Tuple2<IComponentInstance, IComponentAdapter>(mai, mai.getAgentAdapter()));
//	}
	
	/**
	 *  Get the element type.
	 *  @return The element type (e.g. an agent, application or process).
	 * /
	public String getElementType()
	{
		return IComponentFactory.ELEMENT_TYPE_AGENT;
	}*/
	
	/**
	 *  Get the properties.
	 *  Arbitrary properties that can e.g. be used to
	 *  define kernel-specific settings to configure tools.
	 *  @param type	The component type. 
	 *  @return The properties or null, if the component type is not supported by this factory.
	 */
	public Map<String, Object>	getProperties(String type)
	{
		return FILETYPE_MICROAGENT.equals(type)
		? fproperties: null;
	}
	
	/**
	 *  Start the service.
	 * /
	public synchronized IFuture	startService()
	{
		return new Future(null);
	}*/
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 * /
	public synchronized IFuture	shutdownService()
	{
		return new Future(null);
	}*/
	
	/**
	 *  Get the mirco agent class.
	 */
	// todo: make use of cache
	protected Class<?> getMicroAgentClass(String clname, String[] imports, ClassLoader classloader)
	{
		Class<?> ret = SReflect.findClass0(clname, imports, classloader);
//		System.out.println(clname+" "+cma+" "+ret);
		int idx;
		while(ret==null && (idx=clname.indexOf('.'))!=-1)
		{
			clname	= clname.substring(idx+1);
			ret = SReflect.findClass0(clname, imports, classloader);
//			System.out.println(clname+" "+cma+" "+ret);
		}
		if(ret==null)// || !cma.isAssignableFrom(IMicroAgent.class))
			throw new RuntimeException("No micro agent file: "+clname);
		return ret;
	}
	
	/**
	 *  Get the library service
	 */
	protected ILibraryService getLibraryService()
	{
		return internalaccess==null? null: SServiceProvider.getLocalService(internalaccess, ILibraryService.class, RequiredServiceInfo.SCOPE_PLATFORM);
	}
}
