package jadex.rules.examples.manners;
/**
 *  The manners benchmark. 
 */
public class Manners
{
	//-------- type definitions --------
	
	public static final OAVObjectType context_type;
	
	/** A context has a state. */
	public static final OAVAttributeType context_has_state;
	
	
	
	
	
	static
	{
		guest_type = manners_type_model.createType("guest");
		guest_has_name = guest_type.createAttributeType("guest_has_name", OAVJavaType.java_string_type);
		guest_has_sex = guest_type.createAttributeType("guest_has_sex", OAVJavaType.java_string_type);
		guest_has_hobby = guest_type.createAttributeType("guest_has_hobby", OAVJavaType.java_string_type);
	
	/**
	 *  Main for testing. 
	 */
	public static void main(String[] args)
	{