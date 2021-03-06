package jadex.bdi.tutorial;

import java.util.HashMap;
import java.util.Map;

import jadex.bdiv3x.runtime.IMessageEvent;
import jadex.bdiv3x.runtime.Plan;
import jadex.bridge.fipa.SFipa;

/**
 *  An english german translation plan can translate
 *  english words to german and is instantiated on demand.
 */
public class EnglishGermanTranslationPlanB2 extends Plan
{
	//-------- attributes --------

	/** The wordtable. */
	protected Map<String, String> wordtable;

	//-------- methods --------

	/**
	 *  Execute the plan.
	 */
	public void body()
	{
		System.out.println("Created: "+this);

		this.wordtable = new HashMap<String, String>();
		this.wordtable.put("coffee", "Kaffee");
		this.wordtable.put("milk", "Milch");
		this.wordtable.put("cow", "Kuh");
		this.wordtable.put("cat", "Katze");
		this.wordtable.put("dog", "Hund");
		
		String eword = (String)((IMessageEvent)getReason()).getParameter(SFipa.CONTENT).getValue();
		String gword = (String)this.wordtable.get(eword);
		if(gword!=null)
		{
			System.out.println("Translating from english to german: "+eword+" - "+gword);
		}
		else
		{
			System.out.println("Sorry word is not in database: "+eword);
		}
	}
}
