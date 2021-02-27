/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.console;

import static site.sonata.extra2.util.NullUtil.nn;
import static site.sonata.extra2.util.NullUtil.nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import site.sonata.extra2.concurrent.exception.WAInterruptedException;
import site.sonata.extra2.concurrent.exception.WATimeoutException;

/**
 * Class that runs external process, optionally dumps output to System.out,
 * and can send commands to the running process.
 * The process is started immediately on creation.
 * 
 * This class can optionally collect process output for examination.
 * 
 * This class is supposed to be platform-independent -- at least in so far as
 * it should work on Unix and Windows.
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public class ExternalProcess
{
	/**
	 * Marker used to mark 'end of process output' in {@link #outputQueue}
	 */
	private static final String END_PROCESS_OUTPUT_MARKER = "end-process-output-marker-129uihsdcfjklnxcv,n 79y349yvknkash79y1ry iv.h sdlf y9034 4 hl fhf,,.nv89 y 230t wb ,.vb ,234yt 239hgkl v,fv9ovnnfvl v78923 ghiobjkbnc,dv923 ygn";
	
	/**
	 * Process being run.
	 */
	private final Process process;
	
	/**
	 * Process writer (where we can write commands).
	 */
	private final PrintWriter processWriter;
	
	/**
	 * Whether we should dump output.
	 */
	private final boolean dumpOutput;
	
	/**
	 * Whether we collect output.
	 */
	private final boolean collectOutput;
	
	/**
	 * Queue for storing process output (if retrieval is enabled) -- when process 
	 * finishes (or output not collected), {@link #END_PROCESS_OUTPUT_MARKER} 
	 * is put into this queue in order to mark the event (and it is kept inside 
	 * at all times in order to be able to finish subsequent polls). 
	 */
	private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<String>();
	
	/**
	 * Constructor.
	 */
	public ExternalProcess(File workingDir, boolean dumpOutput, boolean collectOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		this.process = nn(Console.prepareExternalProcess(workingDir, dumpOutput, command).start());
		this.dumpOutput = dumpOutput;
		this.collectOutput = collectOutput;
		this.processWriter = new PrintWriter(process.getOutputStream());
		
		// Create thread that will be reading and optionally dumping output.
		new Thread()
		{

			/* (non-Javadoc)
			 * @see java.lang.Thread#run()
			 */
			@Override
			public void run()
			{
				try
				{
					PartialLineReader reader = new PartialLineReader(process.getInputStream());
					String line;
					while((line = reader.readLine()) != null)
					{
						if (ExternalProcess.this.dumpOutput)
							System.out.println(line);
						if (ExternalProcess.this.collectOutput)
							outputQueue.put(line);
					}
					
					if (ExternalProcess.this.dumpOutput)
					{
						System.out.println("Exit code: " + Console.getExitCode(process));
						System.out.println("---------------------------------------------------------");
					}
				} catch (Exception e)
				{
					e.printStackTrace();
				} finally
				{
					if (ExternalProcess.this.collectOutput)
					{
						try
						{
							outputQueue.put(END_PROCESS_OUTPUT_MARKER);
						} catch( InterruptedException e )
						{
							e.printStackTrace(); // Should never happen really
						}
					}
				}
			}
			
		}.start(); // And launch console reading / dumping. Reading is necessary in any case to prevent process blocking.
		
		// If output is not collected, mark the end in queue.
		if (!collectOutput)
		{
			try
			{
				outputQueue.put(END_PROCESS_OUTPUT_MARKER);
			} catch( InterruptedException e )
			{
				throw new WAInterruptedException(e); // Should never happen really.
			}
		}
	}
	
	/**
	 * Constructor.
	 * Doesn't collect output for processing.
	 */
	public ExternalProcess(File workingDir, boolean dumpOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		this(workingDir, dumpOutput, false, command);
	}
	
	/**
	 * Constructor.
	 * Doesn't collect output for processing.
	 */
	public ExternalProcess(String workingDir, boolean dumpOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		this(new File(workingDir), dumpOutput, command);
	}
	
	/**
	 * Constructor.
	 */
	public ExternalProcess(String workingDir, boolean dumpOutput, boolean collectOutput, String... command)
		throws IllegalArgumentException, IOException
	{
		this(new File(workingDir), dumpOutput, collectOutput, command);
	}
	
	/**
	 * Sends line to process (finishes it with cr/lf as per platform).
	 */
	public synchronized void sendLineToProcess(String line)
	{
		processWriter.println(line);
		processWriter.flush();
	}
	
	/**
	 * Sends partial line to process (DOES NOT finish it with cr/lf).
	 */
	public synchronized void sendPartialLineToProcess(String line)
	{
		processWriter.print(line);
		processWriter.flush();
	}

    /**
     * Causes the current thread to wait, if necessary, until the 
     * process represented by this <code>Process</code> object has 
     * terminated. This method returns 
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     *
     * @return     the exit value of the process. By convention, 
     *             <code>0</code> indicates normal termination.
     * @exception  WAInterruptedException  if the current thread is 
     *             {@linkplain Thread#interrupt() interrupted} by another
     *             thread while it is waiting, then the wait is ended and
     *             an {@link InterruptedException} is thrown.
     */
    public int waitFor() throws WAInterruptedException
    {
    	try
		{
			return process.waitFor();
		} catch( InterruptedException e )
		{
			throw new WAInterruptedException(e);
		}
    }

    /**
     * Causes the current thread to wait, if necessary, until the 
     * process represented by this <code>Process</code> object has 
     * terminated. This method returns 
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     *
     * @return     the exit value of the process. By convention, 
     *             <code>0</code> indicates normal termination.
     * @exception  WAInterruptedException  if the current thread is 
     *             {@linkplain Thread#interrupt() interrupted} by another
     *             thread while it is waiting, then the wait is ended and
     *             an {@link InterruptedException} is thrown.
     * @exception WATimeoutException if timed out while waiting           
     */
    public int waitFor(long timeout) throws WAInterruptedException, WATimeoutException
    {
    	return waitFor(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Causes the current thread to wait, if necessary, until the 
     * process represented by this <code>Process</code> object has 
     * terminated. This method returns 
     * immediately if the subprocess has already terminated. If the
     * subprocess has not yet terminated, the calling thread will be
     * blocked until the subprocess exits.
     *
     * @return     the exit value of the process. By convention, 
     *             <code>0</code> indicates normal termination.
     * @exception  WAInterruptedException  if the current thread is 
     *             {@linkplain Thread#interrupt() interrupted} by another
     *             thread while it is waiting, then the wait is ended and
     *             an {@link InterruptedException} is thrown.
     * @exception WATimeoutException if timed out while waiting           
     */
    public int waitFor(long timeout, TimeUnit timeUnit) throws WAInterruptedException, WATimeoutException
    {
    	try
		{
			if (!process.waitFor(timeout, timeUnit))
    			throw new WATimeoutException("Timed out waiting on process completion: " + timeout + "ms");
			return process.exitValue();
		} catch( InterruptedException e )
		{
			throw new WAInterruptedException(e);
		}
    }

    /**
     * Returns the exit value for the subprocess.
     *
     * @return  the exit value of the subprocess represented by this 
     *          <code>Process</code> object. by convention, the value 
     *          <code>0</code> indicates normal termination.
     * @exception  IllegalThreadStateException  if the subprocess represented 
     *             by this <code>Process</code> object has not yet terminated.
     */
    public int exitValue()
    {
    	return Console.getExitCode(process);
    }

    /**
     * Kills the subprocess. The subprocess represented by this 
     * <code>Process</code> object is forcibly terminated.
     */
    public void destroy()
    {
    	Console.destroyProcessAndChildren(process);
    }
    
    /**
     * Checks whether process is still alive.
     */
    public boolean isAlive()
    {
    	try
    	{
    		process.exitValue();
    		return false;
    	} catch (IllegalThreadStateException e)
    	{
    		return true;
    	}
    }
    
    /**
     * If output collection is enabled -- this method will return output lines.
     * Note that it will also return partial lines after a small time-out (this
     * prevents process from 'hanging' indefinitely on incomplete lines).
     * 
     * @return next output line or null if there's no more (or output is not collected)
     */
    @Nullable
    public String readOutputLine() throws WAInterruptedException
    {
    	return internalReadOutputLine(-1);
    }
    
    /**
     * If output collection is enabled -- this method will return output lines.
     * Note that it will also return partial lines after a small time-out (this
     * prevents process from 'hanging' indefinitely on incomplete lines).
     * 
     * @return next output line or null if there's no more (or output is not collected)
     * 
     * @throws WATimeoutException if line is not available during the timeout
     */
    @Nullable
    public String readOutputLine(long timeout) throws WAInterruptedException, 
    	WATimeoutException, IllegalArgumentException
    {
    	if (timeout < 0)
    		throw new IllegalArgumentException("Timeout must not be negative, got: " + timeout);
    	
    	return internalReadOutputLine(timeout);
    }
    
    /**
     * If output collection is enabled -- this method will return output lines.
     * Note that it will also return partial lines after a small time-out (this
     * prevents process from 'hanging' indefinitely on incomplete lines).
     * 
     * @param timeout how long to wait, -1 means indefinitely
     * 
     * @return next output line or null if there's no more (or output is not collected)
     * 
     * @throws WATimeoutException if line is not available during the timeout
     * 		(only if timeout is not set to 'indefinite')
     */
    @Nullable
    private String internalReadOutputLine(long timeout) throws WAInterruptedException, WATimeoutException
    {
    	final String result;
    	try
    	{
    		if (timeout == -1)
    			result = outputQueue.take();
    		else
    			result = nullable(outputQueue.poll(timeout, TimeUnit.MILLISECONDS));
    		if (result == null)
    			throw new WATimeoutException("Timed out waiting on process output line: " + timeout + "ms");
	    	if (result == END_PROCESS_OUTPUT_MARKER)
	    	{
	    		// No more lines, so make sure we put marker back.
	    		outputQueue.put(result);
	    		return null;
	    	}
    	} catch (InterruptedException e)
    	{
    		throw new WAInterruptedException(e);
    	}
    	
    	return result;
    }
}
