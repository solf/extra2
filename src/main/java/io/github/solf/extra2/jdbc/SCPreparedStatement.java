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
package io.github.solf.extra2.jdbc;

import static io.github.solf.extra2.util.NullUtil.fakeVoid;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;

import io.github.solf.extra2.jdbc.SqlClient.SQLConsumer;
import io.github.solf.extra2.jdbc.SqlClient.SQLFunction;
import lombok.AllArgsConstructor;
import lombok.ToString;

/**
 * A version of SQL {@link PreparedStatement} that works with {@link SqlClient}
 * instance.
 *
 * @author Sergey Olefir
 */
@NonNullByDefault
@AllArgsConstructor
@ToString
public class SCPreparedStatement
{
	/**
	 * {@link SqlClient} this prepared statement is for.
	 */
	private final SqlClient sqlClient;
	
	/**
	 * SQL statement to be executed.
	 */
	private final String statementSql;
	
	/**
	 * null value means {@link #resultSetType}, {@link #resultSetConcurrency}
	 * and {@link #resultSetHoldability} are not specified (all of them).
	 * <p>
     * resultSetType a result set type; one of
     *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
	 */
	@Nullable
	private final Integer resultSetType;
	
	/**
     * resultSetConcurrency a concurrency type; one of
     *         <code>ResultSet.CONCUR_READ_ONLY</code> or
     *         <code>ResultSet.CONCUR_UPDATABLE</code>
	 */
	private final int resultSetConcurrency;
	
	/**
	 * Null value means not-specified.
	 * <p>
     * resultSetHoldability one of the following <code>ResultSet</code>
     *        constants:
     *         <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
	 * 
	 */
	@Nullable
	private final Integer resultSetHoldability;
	
	/**
	 * Code block for setting up prepared statement parameters.
	 */
	private final SQLConsumer<PreparedStatement> setupParameters;
	
	/**
	 * Proxy for {@link PreparedStatement} that tracks all the method invocations
	 * in the provided trace list.
	 * <p>
	 * Used to track what arguments were passed to the {@link PreparedStatement}
	 * to be outputted in case of an exception.
	 */
	private static class PreparedStatementProxy implements InvocationHandler
	{
		/**
		 * {@link PreparedStatement} instance being proxied.
		 */
		private final PreparedStatement ps;
		
		/**
		 * Trace list of all method invocations.
		 */
		private final List<Pair<Method, @Nullable Object @Nullable []>> trace;
		
		/**
		 * Constructor (explicit to address nullability issues).
		 */
		public PreparedStatementProxy(PreparedStatement ps,
			List<Pair<Method, @Nullable Object @Nullable []>> trace)
		{
			super();
			this.ps = ps;
			this.trace = trace;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method,
			@Nullable Object @Nullable [] args)
			throws Throwable
		{
			trace.add(new Pair<>(method, args));
			
			try
			{
				return method.invoke(ps, args);
			} catch (InvocationTargetException ite)
			{
				Throwable cause = ite.getCause();
				if (cause != null)
					throw cause;
				
				throw ite;
			}
		}
	}
	
    /**
     * Executes SQL statement
     * which must be an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     */	
	public int executeUpdate()
		throws SQLException
	{
		return withStatement(ps -> sqlClient.executeUpdate(ps));
	}

    /**
     * Executes SQL statement
     * which must be an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     *
     * @param keysRS handler for the resulting generated keys {@link ResultSet}
     * 		(should extract and process generated keys from the given {@link ResultSet}) 
     * 
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     */	
	public int executeUpdateProcessGeneratedKeys(SQLConsumer<ResultSet> keysRS)
		throws SQLException
	{
		return withStatement(ps -> sqlClient.executeUpdateProcessGeneratedKeys(ps, keysRS), true);
	}
	
	/**
	 * Runs given query and executes provided lambda for the ResultSet + returns a result:
	 * <p>
	 * int count = sqlClient.executeQuery("SELECT id FROM table WHERE id < 100", rs ->
	 * 	{
	 * 		int rows = 0;
	 *		while (rs.next())
	 *		{
	 *			rows++;
	 *		}
	 *		return rows;
	 *	}
	 * );
	 *
	 * @see #forEachRow(SQLConsumer)
	 */
	public <R> R executeQuery(SQLFunction<ResultSet, R> rs)
		throws SQLException
	{
		return withStatement(ps -> sqlClient.executeQuery(ps, rs));
	}

	/**
	 * Runs given query and executes provided lambda for the ResultSet e.g.:
	 * <p>
	 * sqlClient.executeQuery("SELECT id FROM table WHERE id < 100", rs ->
	 * 	{
	 *		while (rs.next())
	 *		{
	 *			results.add(rs.getInt(1));
	 *		}
	 *	}
	 * );
	 * 
	 * @see #forEachRow(SQLConsumer)
	 */
	public void executeQueryNoReturnValue(SQLConsumer<ResultSet> rs)
		throws SQLException
	{
		withStatementNoValue(ps -> sqlClient.executeQueryNoReturnValue(ps, rs));
	}

	/**
	 * Runs given query and executes provided lambda for each row in the result
	 * set, e.g.:
	 * <p>
	 * sqlClient.forEachRow("SELECT id FROM table WHERE id < 100", row -> { 
	 * 		results.add(row.getInt(1));
	 * });
	 * 
	 * @return number of rows processed
	 */
	public int forEachRow(SQLConsumer<ResultSet> row)
		throws SQLException
	{
		return withStatement(ps -> sqlClient.forEachRow(ps, row));
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * no rows or more than one row, e.g.:
	 * <p>
	 * sqlClient.forSingleRow("SELECT count(*) FROM table WHERE id < 100", row -> { 
	 * 		return row.getInt(1);
	 * });
	 * 
	 * @return resulting function value
	 */
	public <R> R forSingleRow(SQLFunction<ResultSet, R> row)
		throws SQLException
	{
		return withStatement(ps -> sqlClient.forSingleRow(ps, row));
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * no rows or more than one row, e.g.:
	 * <p>
	 * AtomicLong count = new AtomicLong();
	 * sqlClient.forSingleRow("SELECT count(*) FROM table WHERE id < 100", row -> { 
	 * 		count.set(row.getLong(1));
	 * });
	 * 
	 * @return resulting function value
	 */
	public void forSingleRowNoReturnValue(SQLConsumer<ResultSet> row)
		throws SQLException
	{
		withStatementNoValue(ps -> sqlClient.forSingleRowNoReturnValue(ps, row));
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * more than one row; returns null if result contains no rows, e.g.:
	 * <p>
	 * sqlClient.forZeroOrOneRow("SELECT count(*) FROM table WHERE id < 100", row -> { 
	 * 		return row.getInt(1);
	 * });
	 * 
	 * @return resulting function value or null if there are no result rows
	 */
	@Nullable
	public <R> R forZeroOrOneRow(SQLFunction<ResultSet, R> row)
		throws SQLException
	{
		return withStatement(ps -> sqlClient.forZeroOrOneRow(ps, row));
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * more than one row; does nothing if result contains no rows, e.g.:
	 * <p>
	 * sqlClient.forZeroOrOneRow("SELECT count(*) FROM table WHERE id < 100", row -> { 
	 * 		return row.getInt(1);
	 * });
	 * 
	 * @return true if there was one row, false if there were no rows
	 */
	public boolean forZeroOrOneRowNoReturnValue(SQLConsumer<ResultSet> row)
		throws SQLException
	{
		return withStatement(ps -> sqlClient.forZeroOrOneRowNoReturnValue(ps, row));
	}
	
	/**
	 * Creates {@link PreparedStatement} based on {@link #statementSql} and
	 * executes the provided handler with this statement. {@link PreparedStatement}
	 * is closed at the end.
	 */ 
	private <R> R withStatement(SQLFunction<PreparedStatement, R> ps)
		throws SQLException
	{
		return withStatement(ps, false);
	}
	
	/**
	 * Creates {@link PreparedStatement} based on {@link #statementSql} and
	 * executes the provided handler with this statement. {@link PreparedStatement}
	 * is closed at the end.
	 * 
	 * @param returnGeneratedKeys if true, then {@link PreparedStatement} is
	 * 		created with {@link Statement#RETURN_GENERATED_KEYS} flag (and
	 * 		it's expected that the handler will make use of the generated keys)
	 */
	private <R> R withStatement(SQLFunction<PreparedStatement, R> ps, final boolean returnGeneratedKeys)
		throws SQLException
	{
		final List<Pair<Method, @Nullable Object @Nullable []>> trace = new ArrayList<>();
		try
		{
			@SuppressWarnings("resource") PreparedStatement statement = createPreparedStatement(returnGeneratedKeys);
			try
			{
				final Object statementProxy;
				if (sqlClient.isTrackPreparedStatementParameters())
				{
					PreparedStatementProxy proxy = new PreparedStatementProxy(statement, trace);
					statementProxy = Proxy.newProxyInstance(SCPreparedStatement.class.getClassLoader(), 
						new Class<?>[] {PreparedStatement.class},
						proxy);
				}
				else
					statementProxy = statement; // no proxying in this case, use directly
				
				
				// Setup prepared statement parameters
				setupParameters.accept((PreparedStatement)statementProxy);
				
				return ps.apply(statement);
			} finally
			{
				try
				{
					statement.close();
				} catch (SQLException e)
				{
					sqlClient.handlePreparedStatementCloseException(e);
				}
			}
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: [" + statementSql + "]; " + (sqlClient.isTrackPreparedStatementParameters() ? "args: " + buildArgsString(trace) : "args not tracked"), e);
		}
	}

	/**
	 * Creates {@link PreparedStatement} for use.
	 * 
	 * @param returnGeneratedKeys if true, then {@link PreparedStatement} is
	 * 		created with {@link Statement#RETURN_GENERATED_KEYS} flag (and
	 * 		it's expected that the handler will make use of the generated keys)
	 */
	@SuppressWarnings("deprecation")
	private PreparedStatement createPreparedStatement(final boolean returnGeneratedKeys)
		throws SQLException
	{
		if (returnGeneratedKeys)
			return sqlClient.unsafePrepareStatement(statementSql, Statement.RETURN_GENERATED_KEYS);
		
		Integer rst = resultSetType;
		Integer rsH = resultSetHoldability;
		
		if (rst == null)
			return sqlClient.unsafePrepareStatement(statementSql);
		
		if (rsH == null)
			return sqlClient.unsafePrepareStatement(statementSql, rst, resultSetConcurrency);
		else
			return sqlClient.unsafePrepareStatement(statementSql, rst, resultSetConcurrency, rsH);
	}
	
	/**
	 * Creates {@link PreparedStatement} based on {@link #statementSql} and
	 * executes the provided handler with this statement. {@link PreparedStatement}
	 * is closed at the end.
	 * <p>
	 * This is a version of the method that has no return value.
	 */ 
	private void withStatementNoValue(SQLConsumer<PreparedStatement> ps)
		throws SQLException
	{
		withStatement(rps -> {ps.accept(rps); return fakeVoid();});
	}
	
	/**
	 * Builds user-friendly string of method invocations done (and their
	 * arguments) based on the provided trace list.
	 */
	private static String buildArgsString(List<Pair<Method, @Nullable Object @Nullable []>> trace)
	{
		if (trace.size() == 0)
			return "[]";
		
		StringBuilder sb = new StringBuilder(50);
		for (Pair<Method, @Nullable Object @Nullable []> item : trace)
		{
			sb.append((sb.length() > 0) ? ", " : "[");
			
			sb.append(item.getValue0().getName());
			sb.append(Arrays.toString(item.getValue1()));
		}
		
		sb.append(']');
		
		return sb.toString();
	}
}
