/**
 * Copyright Sergey Olefir
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.solf.extra2.concurrent;

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
