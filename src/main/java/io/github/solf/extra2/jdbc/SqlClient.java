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

import static io.github.solf.extra2.collection.WACollections.toIterable;
import static io.github.solf.extra2.util.NullUtil.fakeNonNull;
import static io.github.solf.extra2.util.NullUtil.nnChecked;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;

import lombok.Getter;

/**
 * Helpful functions for working with SQL via JDBC.
 * <p>
 * 2023-01-30 this ought to handle {@link SQLException} when auto-closing {@link ResultSet}, but not doing it right now.
 */
@NonNullByDefault
public class SqlClient implements AutoCloseable
{
	private final Connection connection;
	
	/**
	 * If true, then {@link SCPreparedStatement} will track the values set into
	 * {@link PreparedStatement} (by using dynamic proxies) in order to add these
	 * values to the exception information if one occurs (should make figuring
	 * out what went wrong with the SQL much easier).
	 */
	@Getter
	private final boolean trackPreparedStatementParameters;
	
	/**
	 * Creates a {@link SqlClient} for the given data source.
	 * <p>
	 * Connection is obtained from the data source immediately and is released
	 * when this {@link SqlClient} is closed.
	 * <p>
	 * {@link #trackPreparedStatementParameters} is set to true
	 */
	public SqlClient(DataSource dataSource) throws SQLException
	{
		this(dataSource, true);
	}
	
	/**
	 * Creates a {@link SqlClient} for the given data source.
	 * <p>
	 * Connection is obtained from the data source immediately and is released
	 * when this {@link SqlClient} is closed.
	 * 
	 * @param trackPreparedStatementParameters if true, then {@link SCPreparedStatement} will track the values set into
	 * {@link PreparedStatement} (by using dynamic proxies) in order to add these
	 * values to the exception information if one occurs (should make figuring
	 * out what went wrong with the SQL much easier).
	 */
	@SuppressWarnings("all") // suppress 'all' rather than 'resource' for warning-compatibility with older Eclipse
	public SqlClient(DataSource dataSource, boolean trackPreparedStatementParameters) throws SQLException
	{
		this(dataSource.getConnection(), trackPreparedStatementParameters);
	}
	
	/**
	 * Create a {@link SqlClient} for the given connection.
	 * <p>
	 * {@link #trackPreparedStatementParameters} is set to true
	 */
	public SqlClient(Connection connection)
	{
		this(connection, true);
	}
	
	/**
	 * Create a {@link SqlClient} for the given connection.
	 * 
	 * @param trackPreparedStatementParameters if true, then {@link SCPreparedStatement} will track the values set into
	 * {@link PreparedStatement} (by using dynamic proxies) in order to add these
	 * values to the exception information if one occurs (should make figuring
	 * out what went wrong with the SQL much easier).
	 */
	public SqlClient(Connection connection, boolean trackPreparedStatementParameters)
	{
		this.connection = connection;
		this.trackPreparedStatementParameters = trackPreparedStatementParameters;
	}
	
	/**
	 * Creates an {@link SqlClient} for an MySQL connection based on the 
	 * arguments and sets auto-commit to false.
	 * <p>
	 * Login & password are not specified.
	 * <p>
	 * {@link #trackPreparedStatementParameters} is set to true
	 */
	@SuppressWarnings("all") // suppress 'all' rather than 'resource' for warning-compatibility with older Eclipse
	public static SqlClient forMySQL(String host, String db)
		throws IllegalStateException
	{
		return new SqlClient(createMySQLConnectionWithNoAutoCommit(host, db, null, null));
	}
	
	/**
	 * Creates an {@link SqlClient} for an MySQL connection based on the 
	 * arguments and sets auto-commit to false.
	 * <p>
	 * {@link #trackPreparedStatementParameters} is set to true
	 * 
	 * @param login may be null -- in which case it is not specified
	 * @param password may be null -- in which case it is not specified
	 */
	@SuppressWarnings("all") // suppress 'all' rather than 'resource' for warning-compatibility with older Eclipse
	public static SqlClient forMySQL(String host, String db, @Nullable String login, @Nullable String password)
		throws IllegalStateException
	{
		return new SqlClient(createMySQLConnectionWithNoAutoCommit(host, db, login, password));
	}
	
	/**
	 * Creates client for an MySQL connection.
	 * <p>
	 * {@link #trackPreparedStatementParameters} is set to true
	 * 
	 * @deprecated use {@link #forMySQL(String, String)} instead
	 */
	@Deprecated
	public SqlClient(String host, String db)
	{
		try
		{
			final String url = "jdbc:mysql://" + host + "/" + db;
            connection = DriverManager.getConnection(url +
                "?jdbcCompliantTruncation=false" +
                "&dumpQueriesOnException=true" +
                "&useUnicode=true" +
                "&characterEncoding=UTF-8" +
                "&autoReconnect=true" +
                "&useSSL=false");
			connection.setAutoCommit(false);
		} catch( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException("Can't init connection", e);
		}
		
		this.trackPreparedStatementParameters = true;
	}
	

	/**
	 * Creates client for an MySQL connection.
	 * <p>
	 * {@link #trackPreparedStatementParameters} is set to true
	 * 
	 * @deprecated use {@link #forMySQL(String, String, String, String)} instead
	 */
	@Deprecated
	public SqlClient(String host, String db, String login, String password)
	{
		try
		{
			final String url = "jdbc:mysql://" + host + "/" + db;
            connection = DriverManager.getConnection(url +
                "?user=" + login +
                "&password=" + password +
                "&jdbcCompliantTruncation=false" +
                "&dumpQueriesOnException=true" +
                "&useUnicode=true" +
                "&characterEncoding=UTF-8" +
                "&autoReconnect=true" +
                "&useSSL=false");
			connection.setAutoCommit(false);
		} catch( Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException("Can't init connection", e);
		}
		
		this.trackPreparedStatementParameters = true;
	}

	/**
	 * Closes the underlying {@link Connection}
	 * <p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public void close()
	{
		try
		{
			connection.close();
		} catch( SQLException e )
		{
			handleConnectionCloseException(e);
		}
	}

	/**
	 * Commits the underlying {@link Connection}
	 */
	public void commit()
		throws SQLException
	{
		connection.commit();
	}

	/**
	 * Rolls back the underlying {@link Connection}
	 */
	public void rollback()
	{
		try
		{
			if( !connection.getAutoCommit() )
			{
				connection.rollback();
			}
		} catch(SQLException e)
		{
			handleRollbackException(e);
		}
	}
	
	/**
	 * Handles exception during connection closing.
	 * <p>
	 * Does nothing in the base implementation.
	 */
	protected void handleConnectionCloseException(@SuppressWarnings("unused") SQLException exception)
	{
		// does nothing in the base implementation
	}
	
	/**
	 * Handles exception during connection closing.
	 * <p>
	 * Does nothing in the base implementation.
	 */
	protected void handlePreparedStatementCloseException(@SuppressWarnings("unused") SQLException exception)
	{
		// does nothing in the base implementation
	}
	
	/**
	 * Handles exception during rollback.
	 * <p>
	 * Does nothing in the base implementation.
	 */
	protected void handleRollbackException(@SuppressWarnings("unused") SQLException exception)
	{
		// does nothing in the base implementation
	}

    /**
     * Sets the underlying connection's auto-commit mode to the given state.
     * If a connection is in auto-commit mode, then all its SQL
     * statements will be executed and committed as individual
     * transactions.  Otherwise, its SQL statements are grouped into
     * transactions that are terminated by a call to either
     * the method <code>commit</code> or the method <code>rollback</code>.
     * By default, new connections are in auto-commit
     * mode.
     * <P>
     * The commit occurs when the statement completes. The time when the statement
     * completes depends on the type of SQL Statement:
     * <ul>
     * <li>For DML statements, such as Insert, Update or Delete, and DDL statements,
     * the statement is complete as soon as it has finished executing.
     * <li>For Select statements, the statement is complete when the associated result
     * set is closed.
     * <li>For <code>CallableStatement</code> objects or for statements that return
     * multiple results, the statement is complete
     * when all of the associated result sets have been closed, and all update
     * counts and output parameters have been retrieved.
     *</ul>
     * <P>
     * <B>NOTE:</B>  If this method is called during a transaction and the
     * auto-commit mode is changed, the transaction is committed.  If
     * <code>setAutoCommit</code> is called and the auto-commit mode is
     * not changed, the call is a no-op.
     *
     * @param autoCommit <code>true</code> to enable auto-commit mode;
     *         <code>false</code> to disable it
     * @exception SQLException if a database access error occurs,
     *  setAutoCommit(true) is called while participating in a distributed transaction,
     * or this method is called on a closed connection
     * @see #getAutoCommit
     */
	public SqlClient setAutoCommit(boolean value)
		throws SQLException
	{
		connection.setAutoCommit(value);
		return this;
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
	public int executeUpdate(final PreparedStatement ps)
		throws SQLException
	{
		int result = ps.executeUpdate();
		ps.clearParameters();
		
		return result;
	}
	
    /**
     * Executes SQL statement
     * which must be an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     * <code>DELETE</code>; or an SQL statement that returns nothing,
     * such as a DDL statement.
     * <p>
     * The statement must have {@link Statement#RETURN_GENERATED_KEYS} specified
     * during its creation.
     *
     * @param keysRS handler for the resulting generated keys {@link ResultSet}
     * 		(should extract and process generated keys from the given {@link ResultSet}) 
     * 
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     *         or (2) 0 for SQL statements that return nothing
     */	
	public int executeUpdateProcessGeneratedKeys(final PreparedStatement ps, SQLConsumer<ResultSet> keysRS)
		throws SQLException
	{
		int result = ps.executeUpdate();
		
		try (ResultSet krs = ps.getGeneratedKeys())
		{
			keysRS.accept(krs);
		}
		
		ps.clearParameters();
		
		return result;
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
	public int executeUpdate(final String sql)
		throws SQLException
	{
		return withStatement(statement -> statement.executeUpdate(sql));
	}

	/**
	 * Runs multiple update SQLs as a single batch.
	 * 
     * @return an array of update counts containing one element for each
     * command in the batch.  The elements of the array are ordered according
     * to the order in which commands were added to the batch.
	 */
	public int[] executeBatch(String[] sqls)
		throws SQLException
	{
		return withStatement(statement -> {
			for( String sql : sqls )
			{
				statement.addBatch(sql);
			}
			int[] result = statement.executeBatch();
			statement.clearBatch();
			return result;
		});
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
	 * @see #forEachRow(String, SQLConsumer)
	 */
	public <R> R executeQuery(String sql, SQLFunction<ResultSet, R> rs)
		throws SQLException
	{
		try
		{
			return withStatement(statement -> rs.apply(statement.executeQuery(sql)));
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: " + sql, e);
		}
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
	 * @see #forEachRow(String, SQLConsumer)
	 */
	public <R> R executeQuery(PreparedStatement ps, SQLFunction<ResultSet, R> rs)
		throws SQLException
	{
		try
		{
			try (ResultSet _rs = ps.executeQuery())
			{
				return rs.apply(_rs);
			}
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: " + ps, e);
		}
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
	 * @see #forEachRow(String, SQLConsumer)
	 */
	public void executeQueryNoReturnValue(String sql, SQLConsumer<ResultSet> rs)
		throws SQLException
	{
		try
		{
			withStatement(statement -> {
				try (ResultSet _rs = statement.executeQuery(sql))
				{
					rs.accept(_rs);
				}
				return null;
			});
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: " + sql, e);
		}
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
	 * @see #forEachRow(String, SQLConsumer)
	 */
	public void executeQueryNoReturnValue(PreparedStatement ps, SQLConsumer<ResultSet> rs)
		throws SQLException
	{
		try
		{
			try (ResultSet _rs = ps.executeQuery())
			{
				rs.accept(_rs);
			}
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: " + ps, e);
		}
	}
	
	/**
	 * Executes a batch of statements, e.g.:
	 * <p>
	 * sqlClient.executeBatch("INSERT INTO srv_exchange_rate (currency_code, created, rate) VALUES (?, ?, ?)", ps ->
	 * {
	 * 		for (String currencySymbol : JsonObject.getNames(response.getRates()))
	 * 		{
	 *      	double rate = response.getRates().getDouble(currencySymbol);
	 * 			int p = 1;
	 * 			ps.setString(p++, currencySymbol);
	 * 			ps.setTimestamp(p++, updateTimestamp);
	 * 			ps.setDouble(p++, rate);
	 * 			ps.addBatch();
	 * 		}
	 * });
	 * <p>
	 * At the end of execution this method automatically calls:
	 * <p>
	 * ps.executeBatch();
	 * ps.clearBatch();
	 * 
	 * @return array of update values, see {@link PreparedStatement#executeBatch()}
	 */
	public int[] executeBatch(String sql, SQLConsumer<PreparedStatement> ps)
		throws SQLException
	{
		try (PreparedStatement _ps = connection.prepareStatement(sql))
		{
			ps.accept(_ps);
			
			int[] result = _ps.executeBatch();
			_ps.clearBatch();
			
			return result;
		}
		
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
	public int forEachRow(String sql, SQLConsumer<ResultSet> row)
		throws SQLException
	{
		try
		{
			AtomicInteger rows = new AtomicInteger(0);
			withStatement(statement -> {
				try (ResultSet _rs = statement.executeQuery(sql))
				{
					for (ResultSet _row : toIterable(_rs))
					{
						row.accept(_row);
						rows.incrementAndGet();
					}
				}
				return null;
			});
			return rows.get();
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: " + sql, e);
		}
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
	public int forEachRow(PreparedStatement ps, SQLConsumer<ResultSet> row)
		throws SQLException
	{
		try
		{
			int rows = 0;
			try (ResultSet rs = ps.executeQuery())
			{
				for (ResultSet _row : toIterable(rs))
				{
					row.accept(_row);
					rows++;
				}
			}
			return rows;
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; PreparedStatement: " + ps, e);
		}
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * more than one row.
	 * 
	 * @param requireOneRow if true, throws exception if there are no rows at all;
	 * 		if false and there are no rows, then returned Boolean is false and 
	 * 		returned value is undefined
	 * 
	 * @return pair: true if there was one row (false otherwise) & resulting function value
	 */
	private <R> Pair<Boolean, R> forAtMostOneRow(boolean requireOneRow, String sql, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		return withStatement(statement -> 
			forAtMostOneRow(requireOneRow, sql, statement.executeQuery(sql), f));
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * more than one row.
	 * 
	 * @param requireOneRow if true, throws exception if there are no rows at all;
	 * 		if false and there are no rows, then returned Boolean is false and 
	 * 		returned value is undefined
	 * 
	 * @return pair: true if there was one row (false otherwise) & resulting function value
	 */
	private <R> Pair<Boolean, R> forAtMostOneRow(boolean requireOneRow, PreparedStatement ps, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		try (ResultSet rs = ps.executeQuery())
		{
			return forAtMostOneRow(requireOneRow, ps, rs, f);
		}
	}

	/**
	 * Runs given query and executes provided lambda for the only row that is
	 * supposed to be in result set; throws {@link SQLException} if result contains
	 * more than one row.
	 * 
	 * @param requireOneRow if true, throws exception if there are no rows at all;
	 * 		if false and there are no rows, then returned Boolean is false and 
	 * 		returned value is undefined
	 * 
	 * @return pair: true if there was one row (false otherwise) & resulting function value
	 */
	private <R> Pair<Boolean, R> forAtMostOneRow(boolean requireOneRow, Object sqlInfo, ResultSet rs, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		try
		{
			R result = fakeNonNull(); // return value is undefined if zero rows
			int rows = 0;
			for (ResultSet row : toIterable(rs))
			{
				rows++;
				if (rows > 1)
					throw new SQLException("Multiple rows received where exactly one was expected: " + sqlInfo);
				
				result = f.apply(row);
			}
			
			boolean hadOneRow = true;
			if (rows < 1)
			{
				if (requireOneRow)
					throw new SQLException("No rows received where exactly one was expected: " + sqlInfo);
				
				hadOneRow = false;
			}

			return new Pair<Boolean, R>(hadOneRow, result);
		} catch (SQLException e)
		{
			throw new SQLException("" + e.getMessage() + "; sql: " + sqlInfo, e);
		}
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
	public <R> R forSingleRow(String sql, SQLFunction<ResultSet, R> row)
		throws SQLException
	{
		return forAtMostOneRow(true, sql, row).getValue1();
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
	public <R> R forSingleRow(PreparedStatement ps, SQLFunction<ResultSet, R> row)
		throws SQLException
	{
		return forAtMostOneRow(true, ps, row).getValue1();
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
	public void forSingleRowNoReturnValue(String sql, SQLConsumer<ResultSet> row)
		throws SQLException
	{
		forSingleRow(sql, rs -> {row.accept(rs); return null;});
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
	public void forSingleRowNoReturnValue(PreparedStatement ps, SQLConsumer<ResultSet> row)
		throws SQLException
	{
		forSingleRow(ps, rs -> {row.accept(rs); return null;});
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
	public <R> R forZeroOrOneRow(String sql, SQLFunction<ResultSet, R> row)
		throws SQLException
	{
		Pair<Boolean, R> result = forAtMostOneRow(false, sql, row);
		
		if (!result.getValue0())
			return null;
		
		return result.getValue1();
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
	public <R> R forZeroOrOneRow(PreparedStatement ps, SQLFunction<ResultSet, R> row)
		throws SQLException
	{
		Pair<Boolean, R> result = forAtMostOneRow(false, ps, row);
		
		if (!result.getValue0())
			return null;
		
		return result.getValue1();
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
	public boolean forZeroOrOneRowNoReturnValue(String sql, SQLConsumer<ResultSet> row)
		throws SQLException
	{
		return forAtMostOneRow(false, sql, rs -> {row.accept(rs); return null;}).getValue0();
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
	public boolean forZeroOrOneRowNoReturnValue(PreparedStatement ps, SQLConsumer<ResultSet> row)
		throws SQLException
	{
		return forAtMostOneRow(false, ps, rs -> {row.accept(rs); return null;}).getValue0();
	}

	/**
	 * Creates a prepared statement version that works with this {@link SqlClient}
	 * instance.
	 * <p>
	 * This method returns {@link SCPreparedStatement} which takes care of
	 * managing actual {@link PreparedStatement} lifecycle (i.e. it will close
	 * it properly when done).
	 * <p>
	 * Returned {@link SCPreparedStatement} instance has methods similar to the
	 * methods in this {@link SqlClient}, e.g. {@link SCPreparedStatement#forEachRow(SQLConsumer)}
	 * 
     * @param sql an SQL statement that may contain one or more '?' IN
     * 		parameter placeholders
     * @param ps lambda for setting {@link PreparedStatement} parameters once
     * 		the {@link PreparedStatement} instance is actually created 
	 */
	public SCPreparedStatement prepareStatement(String sql, SQLConsumer<PreparedStatement> ps)
	{
		return new SCPreparedStatement(this, sql, 
			null, -1, null, 
			ps);
	}

	/**
	 * Creates a prepared statement version that works with this {@link SqlClient}
	 * instance.
	 * <p>
	 * This method returns {@link SCPreparedStatement} which takes care of
	 * managing actual {@link PreparedStatement} lifecycle (i.e. it will close
	 * it properly when done).
	 * <p>
	 * Returned {@link SCPreparedStatement} instance has methods similar to the
	 * methods in this {@link SqlClient}, e.g. {@link SCPreparedStatement#forEachRow(SQLConsumer)}
	 * 
     * @param sql an SQL statement that may contain one or more '?' IN
     * 		parameter placeholders
     * @param resultSetType a result set type; one of
     *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency a concurrency type; one of
     *         <code>ResultSet.CONCUR_READ_ONLY</code> or
     *         <code>ResultSet.CONCUR_UPDATABLE</code>
     * @param ps lambda for setting {@link PreparedStatement} parameters once
     * 		the {@link PreparedStatement} instance is actually created 
	 */
	public SCPreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, SQLConsumer<PreparedStatement> ps)
	{
		return new SCPreparedStatement(this, sql, 
			resultSetType, resultSetConcurrency, null, 
			ps);
	}

	/**
	 * Creates a prepared statement version that works with this {@link SqlClient}
	 * instance.
	 * <p>
	 * This method returns {@link SCPreparedStatement} which takes care of
	 * managing actual {@link PreparedStatement} lifecycle (i.e. it will close
	 * it properly when done).
	 * <p>
	 * Returned {@link SCPreparedStatement} instance has methods similar to the
	 * methods in this {@link SqlClient}, e.g. {@link SCPreparedStatement#forEachRow(SQLConsumer)}
	 * 
     * @param sql an SQL statement that may contain one or more '?' IN
     * 		parameter placeholders
     * @param resultSetType a result set type; one of
     *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency a concurrency type; one of
     *         <code>ResultSet.CONCUR_READ_ONLY</code> or
     *         <code>ResultSet.CONCUR_UPDATABLE</code>
     * @param resultSetHoldability one of the following <code>ResultSet</code>
     *        constants:
     *         <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @param ps lambda for setting {@link PreparedStatement} parameters once
     * 		the {@link PreparedStatement} instance is actually created 
	 */
	public SCPreparedStatement prepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability, SQLConsumer<PreparedStatement> ps)
	{
		return new SCPreparedStatement(this, sql, 
			resultSetType, resultSetConcurrency, resultSetHoldability, 
			ps);
	}
	
	/**
	 * @deprecated 'unsafe' version of {@link PreparedStatement} -- that is the
	 * 		created statement is not automatically closed, so in most cases 
	 * 		should use {@link #prepareStatement(String, SQLConsumer)} instead
	 */
	@Deprecated
	public PreparedStatement unsafePrepareStatement(String sql)
		throws SQLException
	{
		return connection.prepareStatement(sql);
	}
	
	/**
     * @param autoGeneratedKeys a flag indicating whether auto-generated keys
     *        should be returned; one of
     *        {@link Statement#RETURN_GENERATED_KEYS} or
     *        {@link Statement#NO_GENERATED_KEYS}
     *        
	 * @deprecated 'unsafe' version of {@link PreparedStatement} -- that is the
	 * 		created statement is not automatically closed, so in most cases 
	 * 		should use {@link #prepareStatement(String, SQLConsumer)} instead
	 */
	@Deprecated
	public PreparedStatement unsafePrepareStatement(String sql, int autoGeneratedKeys)
		throws SQLException
	{
		return connection.prepareStatement(sql, autoGeneratedKeys);
	}
	
	/**
     * @param resultSetType a result set type; one of
     *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency a concurrency type; one of
     *         <code>ResultSet.CONCUR_READ_ONLY</code> or
     *         <code>ResultSet.CONCUR_UPDATABLE</code>
     *        
	 * @deprecated 'unsafe' version of {@link PreparedStatement} -- that is the
	 * 		created statement is not automatically closed, so in most cases 
	 * 		should use {@link #prepareStatement(String, SQLConsumer)} instead
	 */
	@Deprecated
	public PreparedStatement unsafePrepareStatement(String sql, int resultSetType,
        int resultSetConcurrency)
        	throws SQLException
	{
		return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
	}
	
	/**
     * @param resultSetType one of the following <code>ResultSet</code>
     *        constants:
     *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @param resultSetConcurrency one of the following <code>ResultSet</code>
     *        constants:
     *         <code>ResultSet.CONCUR_READ_ONLY</code> or
     *         <code>ResultSet.CONCUR_UPDATABLE</code>
     * @param resultSetHoldability one of the following <code>ResultSet</code>
     *        constants:
     *         <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     *        
	 * @deprecated 'unsafe' version of {@link PreparedStatement} -- that is the
	 * 		created statement is not automatically closed, so in most cases 
	 * 		should use {@link #prepareStatement(String, SQLConsumer)} instead
	 */
	@Deprecated
	public PreparedStatement unsafePrepareStatement(String sql, int resultSetType,
        int resultSetConcurrency, int resultSetHoldability)
        	throws SQLException
	{
		return connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
	}

	/**
	 * Attempts to retrieve the list of available tables using connection
	 * metadata.
	 */
	public List<String> getTables()
		throws SQLException
	{
		@Nonnull String[] types = {"TABLE"};
		List<String> tables = new ArrayList<>();
		try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", types))
		{
			while( rs.next() )
			{
				tables.add(nnChecked(rs.getString("TABLE_NAME")));
			}
		}
		return tables;
	}

	/**
	 * Attempts to retrieve the list of columns in the given table by using
	 * connection metadata.
	 */
	public List<ColumnMeta> getColumns(String tableName)
		throws SQLException
	{
		ResultSetMetaData rsmd = executeQuery(
			"SELECT * FROM " + tableName + " LIMIT 0", ResultSet::getMetaData);
		int columnCount = rsmd.getColumnCount();

		List<ColumnMeta> columns = new ArrayList<>();
		for( int i = 1; i <= columnCount; i++ )
		{
			String name = rsmd.getColumnName(i);
			int type = rsmd.getColumnType(i);
			columns.add(new ColumnMeta(name, type));
		}
		return columns;
	}

	/**
	 * Executes some code with the new {@link Statement}; {@link Statement} is
	 * automatically closed afterwards.
	 */
	private <R> R withStatement(SQLFunction<Statement, R> f)
		throws SQLException
	{
		try (Statement statement = connection.createStatement())
		{
			return f.apply(statement);
		}
	}

	/**
	 * Metadata for table column.
	 */
	public static class ColumnMeta
	{
		/**
		 * Column name.
		 */
		private final String name;
		/**
		 * Column type from {@link Types}
		 */
		private final int type;

		ColumnMeta(String name, int type)
		{
			this.name = name;
			this.type = type;
		}

		public String getName()
		{
			return name;
		}

		public int getType()
		{
			return type;
		}
	}

	/**
	 * Version of {@link Function} that can throw {@link SQLException}
	 */
	@FunctionalInterface
	public interface SQLFunction<T, R>
	{

		/**
		 * Applies this function to the given argument.
		 *
		 * @param t the function argument
		 * @return the function result
		 * @throws SQLException
		 */
		R apply(T t)
			throws SQLException;

	}

	/**
	 * Version of {@link Consumer} that can throw {@link SQLException}
	 */
	@FunctionalInterface
	public interface SQLConsumer<T>
	{

		/**
		 * Applies this function to the given argument.
		 *
		 * @param t the function argument
		 * @throws SQLException
		 */
		void accept(T t)
			throws SQLException;

	}
	
	/**
	 * Creates an MySQL connection based on the arguments and sets auto-commit
	 * to false.
	 * 
	 * @param login may be null -- in which case it is not specified
	 * @param password may be null -- in which case it is not specified
	 */
	private static Connection createMySQLConnectionWithNoAutoCommit(String host, String db, @Nullable String login, @Nullable String password)
		throws IllegalStateException
	{
		try
		{
			final String url = "jdbc:mysql://" + host + "/" + db;
	        Connection conn = DriverManager.getConnection(url +
	            "?" + 
	        	(login == null ? "" : 
		            "user=" + login +
		            (password == null ? "" : 
		            	"&password=" + password
		            ) + 
		            "&"
		        ) +
	            "jdbcCompliantTruncation=false" +
	            "&dumpQueriesOnException=true" +
	            "&useUnicode=true" +
	            "&characterEncoding=UTF-8" +
	            "&autoReconnect=true" +
	            "&useSSL=false");
			conn.setAutoCommit(false);
			
			return conn;
		} catch( Exception e )
		{
			throw new IllegalStateException("Can't init MySQL connection: " + e, e);
		}
	}
	
}
