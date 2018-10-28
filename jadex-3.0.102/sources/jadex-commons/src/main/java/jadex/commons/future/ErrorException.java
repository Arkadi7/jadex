package jadex.commons.future;

/**
 *  Wrapper to set errors (e.g. ThreadDeath) as future exceptions.
 *  Will be thrown as errors on get(), but passed as wrapped exception in
 *  exceptionOccurred().
 */
public class ErrorException extends Exception
{
	/**
	 *  Create an error exception.
	 */
	public ErrorException(Error error)
	{
		super(error);
	}
	
	/**
	 *  Get the error.
	 */
	public Error	getError()
	{
		return (Error)getCause();
	}
}
