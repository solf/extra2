/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.thread;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Thread implementation that can be signalled to exit via {@link #exitAsap()}
 *
 * @author Sergey Olefir
 */
@ParametersAreNonnullByDefault
public abstract class ExitableThread extends Thread
{
	/**
	 * 
	 */
	public ExitableThread()
	{
		super();
	}

	/**
	 * @param target
	 * @param name
	 */
	public ExitableThread(@Nullable Runnable target, String name)
	{
		super(target, name);
	}

	/**
	 * @param target
	 */
	public ExitableThread(@Nullable Runnable target)
	{
		super(target);
	}

	/**
	 * @param name
	 */
	public ExitableThread(String name)
	{
		super(name);
	}

	/**
	 * @param group
	 * @param target
	 * @param name
	 * @param stackSize
	 */
	public ExitableThread(@Nullable ThreadGroup group, @Nullable Runnable target, String name,
		long stackSize)
	{
		super(group, target, name, stackSize);
	}

	/**
	 * @param group
	 * @param target
	 * @param name
	 */
	public ExitableThread(@Nullable ThreadGroup group, @Nullable Runnable target, String name)
	{
		super(group, target, name);
	}

	/**
	 * @param group
	 * @param target
	 */
	public ExitableThread(@Nullable ThreadGroup group, @Nullable Runnable target)
	{
		super(group, target);
	}

	/**
	 * @param group
	 * @param name
	 */
	public ExitableThread(@Nullable ThreadGroup group, String name)
	{
		super(group, name);
	}

	/**
	 * Signals this thread that it should exit as soon as possible.
	 * <p>
	 * Typical implementation is expected to set some 'exit flag' and interrupt
	 * the thread itself in order to achieve that.
	 */
	public abstract void exitAsap();
}
