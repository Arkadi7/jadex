package jadex.transformation.jsonserializer.processors.read;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.commons.SReflect;
import jadex.commons.transformation.traverser.ITraverseProcessor;
import jadex.commons.transformation.traverser.Traverser;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 * 
 */
public class JsonUUIDProcessor implements ITraverseProcessor
{	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Type type, boolean clone, ClassLoader targetcl)
	{
		Class<?> clazz = SReflect.getClass(type);
		return object instanceof JsonObject && SReflect.isSupertype(UUID.class, clazz);
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	public Object process(Object object, Type type, List<ITraverseProcessor> processors, 
		Traverser traverser, Map<Object, Object> traversed, boolean clone, ClassLoader targetcl, Object context)
	{
		JsonObject obj = (JsonObject)object;
		
		try
		{
			long lsb = obj.getLong("lsb", 0);
			long msb = obj.getLong("msb", 0);
			UUID ret = new UUID(msb, lsb);
//			traversed.put(object, ret);
//			((JsonReadContext)context).addKnownObject(ret);
			
			JsonValue idx = (JsonValue)obj.get(JsonTraverser.ID_MARKER);
			if(idx!=null)
				((JsonReadContext)context).addKnownObject(ret, idx.asInt());
			
			return ret;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
}
