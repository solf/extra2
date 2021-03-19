/**
 * [[[LICENSE-NOTICE]]]
 */
package com.github.solf.extra2.collection;

import static com.github.solf.extra2.util.NullUtil.fakeNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.solf.extra2.util.TypeUtil;

/**
 * A 'sampler' class for items with limited capacity.
 * If new items are added to the sampler after it has reached its capacity,
 * then some of the old items are removed in such a way as to try to preserve
 * samples of events over the entire range of added items.
 * First and last items are always retained.
 * 
 * NOT thread-safe.
 *
 * @author Sergey Olefir
 */
public class ItemSampler<T> implements Cloneable
{
	/**
	 * Maximum capacity for this sampler.
	 */
	private final int capacity;
	
	/**
	 * Internal array keeping the sample data -- this grows as necessary.
	 */
	private ItemHolder<T>[] sampleData;
	
	/**
	 * Total items added.
	 */
	private long totalItemCount;
	
	/**
	 * How many items to read before adding sample to array.
	 */
	private long remainingItemsToRead;
	
	/**
	 * Number of samples collected so far (never exceeds {@link #capacity})
	 */
	private int collectedSamplesCount;
	
	/**
	 * Last item added.
	 */
	private T lastItemAdded;
	
	/**
	 * Cached sample list.
	 */
	private @Nullable List<T> cachedSampleList;
	
	/**
	 * Index for next (probable) deletion in {@link #sampleData}
	 */
	private int nextSampleDeletionIndex;
	
	/**
	 * Whether last added item has already been merged into sample array.
	 */
	private boolean lastItemMerged = true;
	
	/**
	 * Class for tracking item together with the position (sequence) it was added at. 
	 */
	private static class ItemHolder<T>
	{
		/**
		 * Item.
		 */
		final public T item;
		
		/**
		 * Position/sequence number it was added at -- this starts at 1 and increments
		 * for each invocation of {@link ItemSampler#add(Object)}
		 */
		final public long position;
		
		/**
		 * Constructor.
		 */
		public ItemHolder(long position, T item)
		{
			this.position = position;
			this.item = item;
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * @param capacity how many samples are kept (can be less than that if
	 * 		less items are added); must be 2 or more
	 */
	public ItemSampler(int capacity) throws IllegalArgumentException
	{
		if (capacity < 2)
			throw new IllegalArgumentException("Capacity must be 2 or more, got: " + capacity);
		
		this.capacity = capacity;
		this.sampleData = TypeUtil.coerce(new ItemHolder[capacity]);
		
		this.nextSampleDeletionIndex = capacity - 2; // First target for deletion is the next-to-last item.
		
		this.totalItemCount = 0;
		this.collectedSamplesCount = 0;
		this.remainingItemsToRead = 1;
		
		this.lastItemAdded = fakeNonNull();
		this.cachedSampleList = null;
	}
	
	/**
	 * Adds an item.
	 */
	public void add(T item)
	{
		cachedSampleList = null; // reset cache.
		totalItemCount++;
		lastItemAdded = item;
		lastItemMerged = false;
		
		remainingItemsToRead--;
		if (remainingItemsToRead > 0)
			return; // easy case, still skipping some items.
		
		
		// Here we need to adjust sampling array.
		ItemHolder<T> holder = new ItemHolder<>(totalItemCount, item);
		
		// If we don't have enough samples yet -- just record new sample.
		if (collectedSamplesCount < capacity)
		{
			sampleData[collectedSamplesCount] = holder;
			collectedSamplesCount++;
			remainingItemsToRead = calculateItemsToRead();
			lastItemMerged = true;
			return;
		}
		
		// Special-case sample size = 2
		if (capacity == 2)
		{
			remainingItemsToRead = Long.MAX_VALUE; // For 2 items we just need first and last -- which is kept track of anyway.
			return;
		}
		
		// Need to free up space in sampling array.
		int index = deleteArrayElement(sampleData);
		nextSampleDeletionIndex = index - 2; // Most likely target for next deletion.
		if (nextSampleDeletionIndex < 1)
			nextSampleDeletionIndex = capacity - 2; // Never delete starting item and after wrap-around best target is next-to-last
		
		sampleData[capacity - 1] = holder; // record new element at the end.
		lastItemMerged = true;
		
		remainingItemsToRead = calculateItemsToRead();
		
		return;
	}
	
	/**
	 * Looks up a best deletion point in the array.
	 * Verifies items excluding the first and the last item.
	 * Seeks first match that will result in LESS distance (distance is difference
	 * between {@link ItemHolder#position} of neighboring elements AFTER the
	 * deletion at the given index) than given argument.
	 * Search starts at the given index and goes backwards (toward the start
	 * of the array) and loops around if needed.
	 * 
	 * @return found index or -1 if there's no match with less distance than
	 * 		the given argument
	 */
	private int findDeletionPoint(int searchStartIndex, ItemHolder<T>[] array, long distanceLimit)
	{
		for (int i = 0; i < array.length - 2; i++) // minus 2 because we skip first and last elements.
		{
			int index = searchStartIndex - i;
			if (index < 1)
				index += array.length - 2; // minus 2 because we skip last element
			
			long distance = array[index + 1].position - array[index - 1].position;
			if (distance < distanceLimit)
				return index;
		}
		
		return -1;
	}
	
	/**
	 * Looks up a best deletion point using internal state variables (so essentially
	 * for inserting current {@link #lastItemAdded} at the end) BUT targets a
	 * specified array (so that we can perform deletion on array copy for
	 * producing a sample list).
	 * 
	 * @return deletion point (MAY be the last element of the array)
	 */
	private int findDeletionPoint(ItemHolder<T>[] array)
	{
		// Calc distance limit for the last item which is in-flight
		// use -2 to length because we calculate distance for the case when we
		// remove the very last element (so distance is between second-to last and one in-flight)
		long distanceLimit = totalItemCount - array[array.length - 2].position;
		
		int index = findDeletionPoint(nextSampleDeletionIndex, array, distanceLimit);
		if (index < 0)
			index = array.length - 1; // in this case we delete the last element in the array
		
		return index;
	}
	
	/**
	 * Deletes a best item in the given array using internal state variables BUT
	 * targets a specified array so we can perform deletion on array copy for
	 * producing a sample list.
	 * After deletion a new item should be added at the last index
	 * 
	 * @return index at which deletion has occurred; this ought to be recorded
	 * 		in {@link #nextSampleDeletionIndex} if targetting actual sample
	 * 		array; also don't forget {@link #lastItemMerged} flag update if needed
	 */
	private int deleteArrayElement(ItemHolder<T>[] array)
	{
		int delIndex = findDeletionPoint(array);
		removeArrayElement(delIndex, array);
		
		return delIndex;
	}
	
	/**
	 * Calculates number of items to read before adding another to sample list.
	 */
	private long calculateItemsToRead()
	{
		return totalItemCount / capacity + 1;
	}

	/**
	 * Gets sample list.
	 * 
	 * NOTE: this is expensive operation, but result is cached, so if you don't
	 * add more items, subsequent invocations are fast.
	 * 
	 * WARNING: calling this method HAS SIDE EFFECTS -- it affects sampling if
	 * {@link #add(Object)} is invoked after this method. So results may
	 * differ between sampler where {@link #getSampleList()} is called once 
	 * at the end and sampler where {@link #getSampleList()} is called between
	 * {@link #add(Object)} invocations even though the items being added
	 * are the same.
	 * 
	 * If that is undesirable, a workaround (at the cost of additional memory
	 * and processing) is using {@link #clone()} method, e.g.:
	 * sampler.clone().getSampleList()
	 * to obtain a current copy of the sample list.
	 * 
	 * @return unmodifiable list with the samples -- contains at most the number
	 * 		of elements specified in the constructor
	 */
	public @Nonnull List<T> getSampleList()
	{
		if (cachedSampleList != null)
			return cachedSampleList;
		
		if (collectedSamplesCount < capacity)
		{
			cachedSampleList = createTList(sampleData, collectedSamplesCount);
			return cachedSampleList;
		}
		
		if (lastItemMerged)
		{
			cachedSampleList = createTList(sampleData, capacity);
			return cachedSampleList;
		}
		
		// Need to merge last item before returning.
		// We'll do remove-merge on a copy
		ItemHolder<T>[] copy = Arrays.copyOf(sampleData, capacity);
		// Special-case: nothing to delete if capacity = 2
		if (capacity > 2)
			deleteArrayElement(copy);
		copy[capacity - 1] = new ItemHolder<T>(totalItemCount, lastItemAdded); // Insert last element.
		
		cachedSampleList = createTList(copy, capacity);
		return cachedSampleList;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	@Nonnull
	public ItemSampler<T> clone()
	{
		try
		{
			ItemSampler<T> result = TypeUtil.coerce(super.clone());
			result.sampleData = result.sampleData.clone();
			
			return result;
		} catch (CloneNotSupportedException e)
		{
			throw new IllegalStateException("ASSERTION FAILED: " + e, e);
		}
	}
	
	
	/**
	 * Removes given item from array.
	 * Mostly stolen from {@link ArrayList#remove(int)}
	 */
	private void removeArrayElement(int index, Object[] array)
	{
        int numMoved = array.length - index - 1;
        if (numMoved > 0)
            System.arraycopy(array, index+1, array, index,
                             numMoved);
	}
	
	/**
	 * Creates an unmodifiable list of T from the first N elements of the given array of holders. 
	 */
	@Nonnull
	private List<T> createTList(ItemHolder<T>[] array, int size)
	{
		Object[] result = new Object[size];
		for (int i = 0; i < size; i++)
			result[i] = array[i].item;
	
		return TypeUtil.coerce(Collections.unmodifiableList(Arrays.asList(result)));
	}
}
