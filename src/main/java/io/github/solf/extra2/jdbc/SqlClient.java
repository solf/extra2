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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.javatuples.Pair;

/**
 * Helpful functions for working with SQL via JDBC.
 */
@ParametersAreNonnullByDefault
public class SqlClient implements AutoCloseable
{
	private final Connection connection;
	
	/**
	 * Create a {@link SqlClient} for the given connection.
	 */
	public SqlClient(Connection connection)
	{
		this.connection = connection;
	}
	
	/**
	 * Creates an {@link SqlClient} for an MySQL connection based on the 
	 * arguments and sets auto-commit to false.
	 * <p>
	 * Login & password are not specified.
	 */
	public static SqlClient forMySQL(String host, String db)
		throws IllegalStateException
	{
		return new SqlClient(createMySQLConnectionWithNoAutoCommit(host, db, null, null));
	}
	
	/**
	 * Creates an {@link SqlClient} for an MySQL connection based on the 
	 * arguments and sets auto-commit to false.
	 * 
	 * @param login may be null -- in which case it is not specified
	 * @param password may be null -- in which case it is not specified
	 */
	public static SqlClient forMySQL(String host, String db, @Nullable String login, @Nullable String password)
		throws IllegalStateException
	{
		return new SqlClient(createMySQLConnectionWithNoAutoCommit(host, db, login, password));
	}
	
	/**
	 * Creates client for an MySQL connection.
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
	}
	

	/**
	 * Creates client for an MySQL connection.
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
	}

	@Override
	public void close()
	{
		try
		{
			connection.close();
		} catch( Exception ignored )
		{
			// ignored
		}
	}

	public void commit()
		throws SQLException
	{
		connection.commit();
	}

	public void rollback()
	{
		try
		{
			if( !connection.getAutoCommit() )
			{
				connection.rollback();
			}
		} catch( Exception ignored )
		{
			// ignored
		}
	}

	public SqlClient setAutoCommit(boolean value)
		throws SQLException
	{
		connection.setAutoCommit(value);
		return this;
	}

	public void updateSQL(final PreparedStatement ps)
		throws SQLException
	{
		ps.execute();
		ps.clearParameters();
	}

	public void updateSQL(final String sql)
		throws SQLException
	{
		withStatement(statement -> statement.execute(sql));
	}

	/**
	 * Runs multiple update SQLs as a single batch.
	 */
	public void updateSQL(String[] sqls)
		throws SQLException
	{
		withStatement(statement -> {
			for( String sql : sqls )
			{
				statement.addBatch(sql);
			}
			statement.executeBatch();
			statement.clearBatch();
			return null;
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
	public <R> R executeQuery(String sql, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		try
		{
			return withStatement(statement -> f.apply(statement.executeQuery(sql)));
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
	public <R> R executeQuery(PreparedStatement ps, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		try
		{
			try (ResultSet rs = ps.executeQuery())
			{
				return f.apply(rs);
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
	public void executeQueryNoReturnValue(String sql, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		try
		{
			withStatement(statement -> {
				f.accept(statement.executeQuery(sql)); 
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
	public void executeQueryNoReturnValue(PreparedStatement ps, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		try
		{
			try (ResultSet rs = ps.executeQuery())
			{
				f.accept(rs);
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
	public int[] executeBatch(String sql, SQLConsumer<PreparedStatement> f)
		throws SQLException
	{
		try (PreparedStatement ps = connection.prepareStatement(sql))
		{
			f.accept(ps);
			
			int[] result = ps.executeBatch();
			ps.clearBatch();
			
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
	public int forEachRow(String sql, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		try
		{
			AtomicInteger rows = new AtomicInteger(0);
			withStatement(statement -> {
				for (ResultSet row : toIterable(statement.executeQuery(sql)))
				{
					f.accept(row);
					rows.incrementAndGet();
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
	public int forEachRow(PreparedStatement ps, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		try
		{
			int rows = 0;
			try (ResultSet rs = ps.executeQuery())
			{
				for (ResultSet row : toIterable(rs))
				{
					f.accept(row);
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
	public <R> R forSingleRow(String sql, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		return forAtMostOneRow(true, sql, f).getValue1();
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
	public <R> R forSingleRow(PreparedStatement ps, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		return forAtMostOneRow(true, ps, f).getValue1();
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
	public void forSingleRowNoReturnValue(String sql, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		forSingleRow(sql, rs -> {f.accept(rs); return null;});
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
	public void forSingleRowNoReturnValue(PreparedStatement ps, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		forSingleRow(ps, rs -> {f.accept(rs); return null;});
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
	public <R> R forZeroOrOneRow(String sql, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		Pair<Boolean, R> result = forAtMostOneRow(false, sql, f);
		
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
	public <R> R forZeroOrOneRow(PreparedStatement ps, SQLFunction<ResultSet, R> f)
		throws SQLException
	{
		Pair<Boolean, R> result = forAtMostOneRow(false, ps, f);
		
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
	public boolean forZeroOrOneRowNoReturn(String sql, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		return forAtMostOneRow(false, sql, rs -> {f.accept(rs); return null;}).getValue0();
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
	public boolean forZeroOrOneRowNoReturn(PreparedStatement ps, SQLConsumer<ResultSet> f)
		throws SQLException
	{
		return forAtMostOneRow(false, ps, rs -> {f.accept(rs); return null;}).getValue0();
	}

	public PreparedStatement prepareStatement(String sql)
		throws SQLException
	{
		return connection.prepareStatement(sql);
	}

	/**
	 * This is javadoc!!
	 * @return
	 * @throws SQLException
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

	private <R> R withStatement(SQLFunction<Statement, R> f)
		throws SQLException
	{
		try (Statement statement = connection.createStatement())
		{
			return f.apply(statement);
		}
	}

	public static class ColumnMeta
	{
		private String name;
		private int type;

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
