/**
 * [[[LICENSE-NOTICE]]]
 */
package site.sonata.extra2.lambda;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

/**
 * Utilities to make working with Lambdas (particularly with stream filters)
 * much more convenient.
 *
 * @author Sergey Olefir
 */
public class LambdaUtil
{
	/**
	 * This simply recast predicate as Predicate -- so you can easily write 
	 * something like this:
	 * <p>
	 * .filter(p(String::isEmpty).or(e -> e.endsWith("suffix")))
	 * <p>
	 * .filter(p((String e) -> e.startsWith("prefix")).and(e -> e.endsWith("suffix")))
	 * <p>
	 * To avoid having to cast arguments like the last example, consider using
	 * 'or'/'and' methods from this class instead.
	 */
	public static <T> Predicate<T> p(Predicate<T> predicate)
	{
		return predicate;
	}

	/**
	 * 'NOT' predicatefor making writing predicate chains much easier, e.g.:
	 * <p>
	 * .filter(
	 * 		not( and(
	 * 			e -> e.contains("e"),
	 * 			e -> e.startsWith("t")
	 * ))
	 */
	public static <T> Predicate<T> not(Predicate<T> target)
	{
		return target.negate();
	}
	
	/**
	 * 'OR' predicate for making writing predicate chains much easier, e.g.:
	 * <p>
	 * .filter(
	 * 		or(
	 * 			e -> e.startsWith("prefix"), 
	 * 			e -> e.isEmpty(), 
	 * 			e -> e.endsWith("suffix"))
	 * )
	 * <p>
	 * NOTE: returns false if predicate list is empty
	 */
	@SafeVarargs
	public static <T> Predicate<T> or(Predicate<T>... predicates)
	{
		return (e) -> 
		{
			for (Predicate<T> p : predicates)
			{
				if (p.test(e))
					return true;
			}
			
			return false;
		};
	}
	
	/**
	 * 'AND' predicate for making writing predicate chains much easier, e.g.:
	 * <p>
	 * .filter(
	 * 		and(
	 * 			e -> e.startsWith("prefix"), 
	 * 			e -> e.endsWith("suffix"))
	 * )
	 * <p>
	 * NOTE: returns false if predicate list is empty
	 */
	@SafeVarargs
	public static <T> Predicate<T> and(Predicate<T>... predicates)
	{
		return (e) -> 
		{
			if (predicates.length == 0)
				return false;
			
			for (Predicate<T> p : predicates)
			{
				if (!p.test(e))
					return false;
			}
			
			return true;
		};
	}
	

	/**
	 * Convenient way to filter streams on evaluated value being equals to any
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAny(e -> e.length(), 3, 4)
	 * )
	 * <p>
	 * .filter(
	 * 		equalsAny(String::length, 3, 4)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T, V> Predicate<T> equalsAny(@Nonnull Function<T, V> valueSupplier, V... options)
	{
		return (t) -> 
		{
			V value = valueSupplier.apply(t);
			
			for (V o : options) 
				if (Objects.equals(value, o)) 
					return true;
			
			return false;
		};
	}

	/**
	 * Convenient way to filter streams on evaluated value being equals to any
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAnyIn(e -> e.length(), validLengths)
	 * )
	 * <p>
	 * .filter(
	 * 		equalsAny(String::length, validLengths)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T, V> Predicate<T> equalsAnyIn(@Nonnull Function<T, V> valueSupplier, @Nonnull Collection<V> options)
	{
		return (t) -> 
		{
			V value = valueSupplier.apply(t);
			
			for (V o : options) 
				if (Objects.equals(value, o)) 
					return true;
			
			return false;
		};
	}

	/**
	 * Convenient way to filter streams on stream value being equals to any
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAny("one", "two", null)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the stream value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T> Predicate<T> equalsAny(T... options)
	{
		return (t) -> 
		{
			for (T o : options) 
				if (Objects.equals(t, o)) 
					return true;
			
			return false;
		};
	}

	/**
	 * Convenient way to filter streams on stream value being equals to any
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAnyIn(validOptions)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the stream value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T> Predicate<T> equalsAnyIn(@Nonnull Collection<T> options)
	{
		return (t) -> 
		{
			for (T o : options) 
				if (Objects.equals(t, o)) 
					return true;
			
			return false;
		};
	}


	/**
	 * Convenient way to filter streams on evaluated result being true for any
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		trueForAny((e, o) -> e.startsWith(o), "o", "f")
	 * )
	 * <p>
	 * .filter(
	 * 		trueForAny(String::startsWith, "o", "f")
	 * )
	 * <p>
	 * To clarify -- arguments given to BiPredicate are stream element (e in examples)
	 * and one of options values (o in examples).
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T, V> Predicate<T> trueForAny(@Nonnull BiPredicate<T, V> evaluator, V... options)
	{
		return (t) -> 
		{
			for (V o : options) 
			{
				if (evaluator.test(t, o)) 
					return true;
			}
			
			return false;
		};
	}


	/**
	 * Convenient way to filter streams on evaluated result being true for any
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		trueForAnyIn((e, o) -> e.startsWith(o), prefixList)
	 * )
	 * <p>
	 * .filter(
	 * 		trueForAnyIn(String::startsWith, prefixList)
	 * )
	 * <p>
	 * To clarify -- arguments given to BiPredicate are stream element (e in examples)
	 * and one of options values (o in examples).
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T, V> Predicate<T> trueForAnyIn(@Nonnull BiPredicate<T, V> evaluator, @Nonnull Collection<V> options)
	{
		return (t) -> 
		{
			for (V o : options) 
			{
				if (evaluator.test(t, o)) 
					return true;
			}
			
			return false;
		};
	}
	
	/**
	 * Convenient way to filter streams on evaluated value being equals to all
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAll(e -> e.length(), 3, 4)
	 * )
	 * <p>
	 * .filter(
	 * 		equalsAll(String::length, 3, 4)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T, V> Predicate<T> equalsAll(@Nonnull Function<T, V> valueSupplier, V... options)
	{
		return (t) -> 
		{
			if (options.length == 0)
				return false;
			
			V value = valueSupplier.apply(t);
			
			for (V o : options) 
				if (!Objects.equals(value, o)) 
					return false;
			
			return true;
		};
	}

	/**
	 * Convenient way to filter streams on evaluated value being equals to all
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAllIn(e -> e.length(), validLengths)
	 * )
	 * <p>
	 * .filter(
	 * 		equalsAll(String::length, validLengths)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T, V> Predicate<T> equalsAllIn(@Nonnull Function<T, V> valueSupplier, @Nonnull Collection<V> options)
	{
		return (t) -> 
		{
			if (options.size() == 0)
				return false;
			
			V value = valueSupplier.apply(t);
			
			for (V o : options) 
				if (!Objects.equals(value, o)) 
					return false;
			
			return true;
		};
	}

	/**
	 * Convenient way to filter streams on stream value being equals to all
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAll("one", "two", null)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the stream value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T> Predicate<T> equalsAll(T... options)
	{
		return (t) -> 
		{
			if (options.length == 0)
				return false;
			
			for (T o : options) 
				if (!Objects.equals(t, o)) 
					return false;
			
			return true;
		};
	}

	/**
	 * Convenient way to filter streams on stream value being equals to all
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		equalsAllIn(validOptions)
	 * )
	 * <p>
	 * NOTE: equals() (if needed) is called on the stream value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T> Predicate<T> equalsAllIn(@Nonnull Collection<T> options)
	{
		return (t) -> 
		{
			if (options.size() == 0)
				return false;
			
			for (T o : options) 
				if (!Objects.equals(t, o)) 
					return false;
			
			return true;
		};
	}


	/**
	 * Convenient way to filter streams on evaluated result being true for all
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		trueForAll((e, o) -> e.startsWith(o), "o", "f")
	 * )
	 * <p>
	 * .filter(
	 * 		trueForAll(String::startsWith, "o", "f")
	 * )
	 * <p>
	 * To clarify -- arguments given to BiPredicate are stream element (e in examples)
	 * and one of options values (o in examples).
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T, V> Predicate<T> trueForAll(@Nonnull BiPredicate<T, V> evaluator, V... options)
	{
		return (t) -> 
		{
			if (options.length == 0)
				return false;
			
			for (V o : options) 
			{
				if (!evaluator.test(t, o)) 
					return false;
			}
			
			return true;
		};
	}


	/**
	 * Convenient way to filter streams on evaluated result being true for all
	 * of the options, e.g.:
	 * <p>
	 * .filter(
	 * 		trueForAllIn((e, o) -> e.startsWith(o), prefixList)
	 * )
	 * <p>
	 * .filter(
	 * 		trueForAllIn(String::startsWith, prefixList)
	 * )
	 * <p>
	 * To clarify -- arguments given to BiPredicate are stream element (e in examples)
	 * and one of options values (o in examples).
	 * <p>
	 * NOTE: equals() (if needed) is called on the evaluated value (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T, V> Predicate<T> trueForAllIn(@Nonnull BiPredicate<T, V> evaluator, @Nonnull Collection<V> options)
	{
		return (t) -> 
		{
			if (options.size() == 0)
				return false;
			
			for (V o : options) 
			{
				if (!evaluator.test(t, o)) 
					return false;
			}
			
			return true;
		};
	}
	
	/**
	 * Checks that given value equals any of the given options, e.g.
	 * <p>
	 * if (valueEqualsAny("string", "s", "a", "d"))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T> boolean valueEqualsAny(T value, T... options)
	{
		for (T o : options) 
			if (Objects.equals(value, o)) 
				return true;
		
		return false;
	}
	
	/**
	 * Checks that given value equals any of the given options, e.g.
	 * <p>
	 * if (valueEqualsAny("string", validPrefixes))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T> boolean valueEqualsAnyIn(T value, Collection<T> options)
	{
		for (T o : options) 
			if (Objects.equals(value, o)) 
				return true;
		
		return false;
	}
	
	
	/**
	 * Checks that evaluated value is true for any of the given options, e.g.
	 * <p>
	 * if (valueTrueForAny(o -> "string".startsWith(o), "s", "a", "d"))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T> boolean valueTrueForAny(Predicate<T> evaluator, T... options)
	{
		for (T o : options) 
		{
			if (evaluator.test(o)) 
				return true;
		}
		
		return false;
	}
	
	/**
	 * Checks that evaluated value is true for any of the given options, e.g.
	 * <p>
	 * if (valueTrueForAnyIn(o -> "string".startsWith(o), allowedPrefixes))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T> boolean valueTrueForAnyIn(Predicate<T> evaluator, Collection<T> options)
	{
		for (T o : options) 
		{
			if (evaluator.test(o)) 
				return true;
		}
		
		return false;
	}
	
	
	/**
	 * Checks that given value equals all of the given options, e.g.
	 * <p>
	 * if (valueEqualsAll("string", "s", "a", "d"))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T> boolean valueEqualsAll(T value, T... options)
	{
		if (options.length == 0)
			return false;
		
		for (T o : options) 
			if (!Objects.equals(value, o)) 
				return false;
		
		return true;
	}
	
	/**
	 * Checks that given value equals all of the given options, e.g.
	 * <p>
	 * if (valueEqualsAll("string", validPrefixes))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T> boolean valueEqualsAllIn(T value, Collection<T> options)
	{
		if (options.size() == 0)
			return false;
		
		for (T o : options) 
			if (!Objects.equals(value, o)) 
				return false;
		
		return true;
	}
	
	
	/**
	 * Checks that evaluated value is true for all of the given options, e.g.
	 * <p>
	 * if (valueTrueForAll(o -> "string".startsWith(o), "s", "a", "d"))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	@SafeVarargs
	public static <T> boolean valueTrueForAll(Predicate<T> evaluator, T... options)
	{
		if (options.length == 0)
			return false;
		
		for (T o : options) 
		{
			if (!evaluator.test(o)) 
				return false;
		}
		
		return true;
	}
	
	/**
	 * Checks that evaluated value is true for all of the given options, e.g.
	 * <p>
	 * if (valueTrueForAllIn(o -> "string".startsWith(o), allowedPrefixes))
	 * <p>
	 * NOTE: equals() (if needed) is called on the value itself (not on option)
	 * <p>
	 * NOTE2: matches nothing if options are empty (contain no items)
	 * <p>
	 * NOTE3: null is equal to null; null is not equal to any non-null
	 */
	public static <T> boolean valueTrueForAllIn(Predicate<T> evaluator, Collection<T> options)
	{
		if (options.size() == 0)
			return false;
		
		for (T o : options) 
		{
			if (!evaluator.test(o)) 
				return false;
		}
		
		return true;
	}
	
}
