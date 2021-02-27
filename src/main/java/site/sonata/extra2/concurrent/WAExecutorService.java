/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Interface for executor implementations that use {@link WAFutureTask} / 
 * {@link WARunnableFutureTask} for tasks so that reference to the task can
 * be obtained from the {@link Future}
 *
 * @author Sergey Olefir
 */
public interface WAExecutorService extends ExecutorService
{
	// No methods yet.
}
