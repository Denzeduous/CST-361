package service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.enterprise.inject.Alternative;

import model.User;

@Alternative
@Stateless
public class DBService implements DBInterface {
	private final String url = "jdbc:postgresql://localhost:5433/Polyopus";
	private 	  String username;
	private 	  String password;
	
	public final int OK    = 0;
	public final int ERROR = -1;
	
	public final int PREFIX   = 1;
	public final int POSTFIX  = 2;
	public final int ANYWHERE = 3;
	
	public final int USERNAME_EXISTS = 1;
	public final int EMAIL_EXISTS    = 2;
	public final int DATABASE_ERROR  = 3;
	
	public DBService(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public DBService() {
		username = "postgres";
		password = "";
	}

	/**
	 * Creates a location (schema.table).
	 * Note that this IS injection safe.
	 * 
	 * @param schema - The schema to use
	 * @param table  - The table to use
	 * 
	 * @return [schema].[table]
	 */
	public String createLocation(String schema, String table) {
		return '"' + schema + '"' + '.' + '"' + table + '"';
	}
	
	/**
	 * Creates a `SET` statement for updates.
	 * NOTE THAT THE NAMES ARE NOT INJECTION SAFE!!!
	 * 
	 * @param names  - A list of the field names
	 * 
	 * @return A `SET` statement in the structure of [name]=?,[name2]=?,...
	 */
	public String set(String[] names) {
		StringBuilder b = new StringBuilder();
		
		for (int i = 0; i < names.length; i++) {
			b.append('"' + names[i] + '"');
			b.append("=?");
			
			if (i != names.length - 1)
				b.append(',');
		}
		
		return b.toString();
	}
	
	/**
	 * Creates a `WHERE` statement (`WHERE [name]=?`).
	 * NOTE THAT THE NAMES ARE NOT INJECTION SAFE!!!
	 * 
	 * @param name - The name of the column
	 * 
	 * @return A `WHERE` statement in the structure of `WHERE [name]=?`
	 */
	public String where(String name) {
		return '"' + name + '"' + "=?";
	}
	
	/**
	 * Creates a conditional chain of statements (call the respective method on each statement before calling this. This simply appends statements and conditionals).
	 * This should be structured like:
	 * 
	 * statement AND/OR statement AND/OR statement...
	 * 
	 * @param statements - An array of the statements
	 * 
	 * @return A conditional chain of statements
	 */
	public String conditionals(String[] statements) {
		StringBuilder b = new StringBuilder();
		
		for (int i = 0; i < statements.length; i++) {
			b.append(statements[i]);
			b.append(' ');
			b.append(statements[++i]);
			
			if (i != statements.length - 1)
				b.append(' ');
		}
		
		return b.toString();
	}
	
	/**
	 * Creates a `LIKE` statement. Note the type is based off of the
	 * location of the value, not the type itself.
	 * 
	 * PREFIX   = 1 (`?%`)
	 * POSTFIX  = 2 (`%?`)
	 * ANYWHERE = 3 (`%?%`)
	 * 
	 * @param fieldName - The name of the field
	 * @param type      - An enum of the type (PREFIX, POSTFIX, ANYWHERE)
	 * 
	 * @return A `LIKE` statement in the structure of `[fieldName] LIKE [?][type][?]`
	 */
	public String like(String fieldName, int type) {
		if (type == PREFIX)
			return '"' + fieldName + '"' + " LIKE 1?";
		
		else if (type == POSTFIX)
			return '"' + fieldName + '"' + " LIKE 2?";
		
		else if (type == ANYWHERE)
			return '"' + fieldName + '"' + " LIKE 3?";
		
		else
			return null;
	}

	/**
	 * Selects a singular object from the database
	 * Pulled from https://stackoverflow.com/a/11826814
	 * 
	 * @param location       - Location to search (use `createLocation`)
	 * @param whereStatement - The `WHERE` statement to search by
	 * @param whereObjects   - The value(s) to search by
	 * 
	 * @return A HashMap of the column names and values, where the values are objects
	 */
	public HashMap<String, Object> select(String location, String whereStatement, Object[] whereObjects) {
		String query = "SELECT * FROM " + location + " WHERE " + whereStatement + " LIMIT 1";
	
		try {
			// The parameter currently being checked for
			//
			// While paramNum is 1-based, its access is not
			//
			// Although it is not actively shown, this is used to stay with
			// PostgreSQL's 1-based standard on parameters in prepared statements
			int paramNum = 1;
			
			// Whether or not we're in a `LIKE` statement
			boolean inLike = false;
			
			// The new query to parse the original query into
			String newQuery = "";
	
			for (int i = 0; i < query.length(); i++) {
				
				// If we're in a `LIKE` statement, we want to switch the boolean value and step the iterator forward 4 steps
				if (i < query.length() - 4)
					if (query.subSequence(i, i + 4).toString().equals("LIKE")) {
						newQuery += "LIKE ";
						
						i += 4;
						
						inLike = true;
					}
				
				// If we're in a `LIKE` statement, get which kind it is from the token (see the `like` method's description
				// for an explanation of the three values.
				//
				// We need to manually alter the value of the object here.
				if (inLike) {
					
					// Prefix
					if (query.charAt(i) == '1') {
						newQuery += "?";
						whereObjects[paramNum - 1] = "%" + (String)whereObjects[paramNum - 1];
						i++;
						inLike = false;
					}
					
					// Postfix
					else if (query.charAt(i) == '2') {
						newQuery += "?";
						whereObjects[paramNum - 1] = (String)whereObjects[paramNum - 1] + "%";
						i++;
						inLike = false;
					}
					
					// Anywhere (prefix AND postfix)
					else if (query.charAt(i) == '3') {
						newQuery += "?";
						whereObjects[paramNum - 1] = "%" + (String)whereObjects[paramNum - 1] + "%";
						i++;
						inLike = false;
					}
				}
				
				else {
					newQuery += query.charAt(i);
				}
				
				if (query.charAt(i) == '?')
					paramNum++;
			}
	
			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(newQuery);
	
			// Iterate through all of the objects for the `WHERE` statement to add them into the prepared statement
			for (int i = 0; i < whereObjects.length; i++) {
				stmt.setObject(i + 1, whereObjects[i]);
			}
	
			// Get the result and its metadata
			ResultSet         res 	   = stmt.executeQuery();
			ResultSetMetaData metaData = res.getMetaData();
			
			int columnCount = metaData.getColumnCount();
			ArrayList<String> columnNames = new ArrayList<String>();
	
			// Iterate over the column meta names and add them to iterate over later
			for (int i = 1; i <= columnCount; i++) {
				columnNames.add(metaData.getColumnName(i));
			}
			
			HashMap<String, Object> objectData = new HashMap<String, Object>();
			
			while (res.next()) {
				
				// Put every column value in using the meta name from before
				for (String name : columnNames) {
					objectData.put(name, res.getObject(name));
				}
			}
			
			stmt.close();
			conn.close();
			
			return objectData;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Selects a singular object from the database. Simple wrapper where you don't have to create a list.
	 * 
	 * @param location       - Location to search (use `createLocation`)
	 * @param whereStatement - The `WHERE` statement to search by
	 * @param whereObject    - The value to search by
	 * 
	 * @return A HashMap of the column names and values, where the values are objects
	 */
	public HashMap<String, Object> select(String location, String whereStatement, Object whereObject) {
		return select(location, whereStatement, new Object[] { whereObject } );
	}
	
	/**
	 * Selects a list of objects from the database
	 * Pulled from https://stackoverflow.com/a/11826814
	 * 
	 * @param location       - Schema and table to search (use `createLocation`)
	 * @param whereStatement - The `WHERE` statement to search by
	 * @param whereObjects   - The value(s) to search by
	 * 
	 * @return An ArrayList of HashMaps of the column names and values, where the values are objects
	 */
	public ArrayList<HashMap<String, Object>> selectMany(String location, String whereStatement, Object[] whereObjects) {
		String query = "SELECT * FROM " + location + " WHERE " + whereStatement;
		
		try {
			// The parameter currently being checked for
			//
			// While paramNum is 1-based, its access is not
			//
			// Although it is not actively shown, this is used to stay with
			// PostgreSQL's 1-based standard on parameters in prepared statements
			int paramNum = 1;
			
			// Whether or not we're in a `LIKE` statement
			boolean inLike = false;
			
			// The new query to parse the original query into
			String newQuery = "";

			for (int i = 0; i < query.length(); i++) {
				
				// If we're in a `LIKE` statement, we want to switch the boolean value and step the iterator forward 4 steps
				if (i < query.length() - 4)
					if (query.subSequence(i, i + 4).toString().equals("LIKE")) {
						newQuery += "LIKE ";
						
						i += 4;
						
						inLike = true;
					}
				
				// If we're in a `LIKE` statement, get which kind it is from the token (see the `like` method's description
				// for an explanation of the three values.
				//
				// We need to manually alter the value of the object here.
				if (inLike) {
					
					// Prefix
					if (query.charAt(i) == '1') {
						newQuery += "?";
						whereObjects[paramNum - 1] = "%" + (String)whereObjects[paramNum - 1];
						i++;
						inLike = false;
					}
					
					// Postfix
					else if (query.charAt(i) == '2') {
						newQuery += "?";
						whereObjects[paramNum - 1] = (String)whereObjects[paramNum - 1] + "%";
						i++;
						inLike = false;
					}
					
					// Anywhere (prefix AND postfix)
					else if (query.charAt(i) == '3') {
						newQuery += "?";
						whereObjects[paramNum - 1] = "%" + (String)whereObjects[paramNum - 1] + "%";
						i++;
						inLike = false;
					}
				}
				
				else {
					newQuery += query.charAt(i);
				}
				
				if (query.charAt(i) == '?')
					paramNum++;
			}

			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(newQuery);

			// Iterate through all of the objects for the `WHERE` statement to add them into the prepared statement
			for (int i = 0; i < whereObjects.length; i++) {
				stmt.setObject(i + 1, whereObjects[i]);
			}
			
			// Get the result and its metadata
			ResultSet         res      = stmt.executeQuery();
			ResultSetMetaData metaData = res.getMetaData();
			
			int columnCount = metaData.getColumnCount();
			ArrayList<String> columnNames = new ArrayList<String>();
			
			// Iterate over the column meta names and add them to iterate over later
			for (int i = 1; i < columnCount; i++) {
				columnNames.add(metaData.getColumnName(i));
			}
			
			ArrayList<HashMap<String, Object>> objects = new ArrayList<HashMap<String, Object>>();

			while (res.next()) {
				HashMap<String, Object> objectData = new HashMap<String, Object>();
				
				// Put every column value in using the meta name from before
				for (String name : columnNames) {
					objectData.put(name, res.getObject(name));
				}

				objects.add(objectData);
			}
			
			stmt.close();
			conn.close();
			
			return objects;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * A wrapper over `selectObjectList` that doesn't make you have to create a list when only checking one object.
	 * 
	 * @param location       - Schema and table to search (use `createLocation`)
	 * @param whereStatement - The `WHERE` statement to search by
	 * @param whereObjects   - The value to search by
	 * 
	 * @return An ArrayList of HashMaps of the column names and values, where the values are objects
	 */
	public ArrayList<HashMap<String, Object>> selectMany(String location, String whereStatement, Object value) {
		return selectMany(location, whereStatement, new Object[] { value } );
	}
	
	@Override
	public ArrayList<HashMap<String, Object>> selectAll(String location) {
		String query = "SELECT * FROM " + location;

		try {
			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// Get the result and its metadata
			ResultSet         res      = stmt.executeQuery();
			ResultSetMetaData metaData = res.getMetaData();
			
			int columnCount = metaData.getColumnCount();
			ArrayList<String> columnNames = new ArrayList<String>();
			
			// Iterate over the column meta names and add them to iterate over later
			for (int i = 1; i <= columnCount; i++) {
				columnNames.add(metaData.getColumnName(i));
			}
			
			ArrayList<HashMap<String, Object>> objects = new ArrayList<HashMap<String, Object>>();

			while (res.next()) {
				HashMap<String, Object> objectData = new HashMap<String, Object>();
				
				// Put every column value in using the meta name from before
				for (String name : columnNames) {
					objectData.put(name, res.getObject(name));
				}

				objects.add(objectData);
			}

			stmt.close();
			conn.close();

			return objects;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Updates a table in the database.
	 * 
	 * @param location       - Schema and table to search (use `createLocation`)
	 * @param setStatement   - The `SET` statement to use (use `set`)
	 * @param setObjects     - The values to set to
	 * @param whereStatement - The `WHERE` statement to search by (use `where`)
	 * @param whereObjects   - The value(s) to search by
	 * 
	 * @return Whether the update was successful
	 */
	public boolean update(String location, String setStatement, Object[] setObjects, String whereStatement, Object[] whereObjects) {
		String query = "UPDATE " + location + " SET " + setStatement + " WHERE " + whereStatement;
		
		try {
			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// Iterate through all of the objects and keep count of where they are
			int objectCount = 1;
			
			for (int i = 0; i < setObjects.length; i++) {
				stmt.setObject(objectCount, setObjects[i]);
				objectCount++;
			}
			
			for (int i = 0; i < whereObjects.length; i++) {
				stmt.setObject(objectCount, whereObjects[i]);
				objectCount++;
			}

			int success = stmt.executeUpdate();

			return success != 0;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Deletes an object from the database.
	 * 
	 * @param location       - Schema and table to search (use `createLocation`)
	 * @param whereStatement - The `WHERE` statement to search by
	 * @param whereObjects   - The value(s) to search by
	 * 
	 * @return Whether the deletion was successful
	 */
	public boolean delete(String location, String whereStatement, Object[] whereObjects) {
		String query = "DELETE FROM " + location + " WHERE " + whereStatement;
		
		try {
			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// Iterate through all of the objects for the `WHERE` statement to add them into the prepared statement
			for (int i = 0; i < whereObjects.length; i++) {
				stmt.setObject(i + 1, whereObjects[i]);
			}

			int success = stmt.executeUpdate();

			return success != 0;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	/**
	 * Inserts a value in the database.
	 * 
	 * @param location   - Schema and table to search (use `createLocation`)
	 * @param fieldNames - An array of the names of each field
	 * @param values     - The actual values to insert
	 * 
	 * @return Whether or not the object was successfully inserted
	 */
	public boolean insert(String location, String[] fieldNames, Object[] values) {
		
		// String builder for field names
		StringBuilder fb = new StringBuilder("INSERT INTO " + location + " (");
		
		// String builder for values
		StringBuilder vb = new StringBuilder(") VALUES (");
		
		for (int i = 0; i < fieldNames.length; i++) {
			fb.append('"' + fieldNames[i] + '"');
			vb.append("?");
			
			if (i != fieldNames.length - 1) {
				fb.append(',');
				vb.append(',');
			}
		}
		
		vb.append(')');
		
		// Finish building the query
		//
		// At the end it should be:
		//
		//     INSERT INTO [location]
		//                 ([fieldNames[0]],[fieldNames[1]],[fieldNames[2]]...)
		//         VALUES
		//                 (?,?,?...)
		String query = fb.toString() + vb.toString();
		
		try {
			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// Iterate through all of the objects for the `WHERE` statement to add them into the prepared statement
			for (int i = 0; i < values.length; i++) {
				stmt.setObject(i + 1, values[i]);
			}

			int success = stmt.executeUpdate();

			return success != 0;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	// TODO: Proper password hashing
	/**
	 * Selects and authenticates the password for the chosen user and authenticates it.
	 * 
	 * @param user - The user to authenticate
	 * 
	 * @return Whether or not the passwords match
	 */
	public boolean authenticate(User user) {
		return user.getPassword().equals((String)select(createLocation("SiteData", "UserDat"),
											  			where("Username"),
											  			new Object[] { user.getUsername() })
									   			.get("Password"));
	}

	/**
	 * Checks whether or not an email exists.
	 * 
	 * @param username - The username to check for
	 * 
	 * @return Whether or not the username was found.
	 */
	public boolean usernameExists(String username) {
		return select(createLocation("SiteData", "UserDat"),
		  	          where("Username"),
		  			  new Object[] { username })
			   .size() != 0;
	}

	/**
	 * Checks whether or not an email exists.
	 * 
	 * @param email - The email to check for
	 * 
	 * @return Whether or not the email was found.
	 */
	public boolean emailExists(String email) {
		return select(createLocation("SiteData", "UserDat"),
	  	          	  where("Email"),
	  	          	  new Object[] { email })
			   .size() != 0;
	}

	/**
	 * Inserts a User object into the database.
	 * 
	 * @param user - The User object to insert into the database. Note that the ID should not be created.
	 * 
	 * @return Whether the email exists, username exists, or whether or not the insertion was successful.
	 * 		   You should check yourself whether or not the username and email exist to get proper coverage.
	 * 		   The username and email check was made as a fail-safe.
	 */
	public int insertUser(User user) {
		if (!usernameExists(user.getUsername())) {
			if (!emailExists(user.getEmail())) {
				boolean ok = insert(createLocation("SiteData", "UserDat"),
						     
								    new String[] {
								 		    "Username",
								   		    "Password",
										    "Email",
								    },
								   
								    new Object[] {
										    user.getUsername(),
										    user.getPassword(),
										    user.getEmail(),
								    }
							 );
				
				return ok ? OK : ERROR;
			}
			
			else {
				return EMAIL_EXISTS;
			}
		}
		
		else {
			return USERNAME_EXISTS;
		}
	}

	/**
	 * Gets a User in the database by its ID.
	 * 
	 * @param userID - The UUID to check for
	 * 
	 * @return The fully complete User object or null on failure.
	 */
	public User getUserbyID(UUID userID) {
		HashMap<String, Object> userValues = select(createLocation("SiteData", "UserDat"),
				                                    "ID", userID.toString());
		
		if (userValues != null) {
			User user = new User(userID,
								 (String)userValues.get("Username"),
								 (String)userValues.get("Password"),
								 (String)userValues.get("Email"));
			return user;
		}
		
		return null;
	}

	/**
	 * Gets a User in the database by its username.
	 * 
	 * @param username - The username to check for
	 * 
	 * @return The fully complete User object or null on failure.
	 */
	public User getUserbyUsername(String username) {
		HashMap<String, Object> userValues = select(createLocation("SiteData", "UserDat"),
		                							where("Username"), username);
		
		if (userValues != null) {
			User user = new User((UUID) userValues.get("UserID"),
								 username,
								 (String)userValues.get("Password"),
								 (String)userValues.get("Email"));
			return user;
		}
		
		return null;
	}

	@Override
	public Object insertAndReturn(String location, String[] fieldNames, Object[] values, String columnToReturn) {
		
		// String builder for field names
		StringBuilder fb = new StringBuilder("INSERT INTO " + location + " (");
		
		// String builder for values
		StringBuilder vb = new StringBuilder(") VALUES (");
		
		// Iterate through each of the field names to add them
		// into the field name section and to add a question mark
		// in the values section
		for (int i = 0; i < fieldNames.length; i++) {
			fb.append('"' + fieldNames[i] + '"');
			vb.append("?");
			
			if (i != fieldNames.length - 1) {
				fb.append(',');
				vb.append(',');
			}
		}
		
		vb.append(") RETURNING ");
		vb.append('"' + columnToReturn + '"');
		
		// Finish building the query
		//
		// At the end it should be:
		//
		//     INSERT INTO [location]
		//                 ([fieldNames[0]],[fieldNames[1]],[fieldNames[2]]...)
		//         VALUES
		//                 (?,?,?...)
		//         RETURNING
		//                 columnToReturn
		String query = fb.toString() + vb.toString();
		
		try {
			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// Iterate through all of the objects for the `WHERE` statement to add them into the prepared statement
			for (int i = 0; i < values.length; i++) {
				stmt.setObject(i + 1, values[i]);
			}

			ResultSet res = stmt.executeQuery();

			Object output = null;
			
			if (res.next())
				output = res.getObject(1);
			
			res .close();
			stmt.close();
			conn.close();
			
			return output;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public int count(String location, String whereStatement, Object[] whereObjects) {
		String query = "SELECT COUNT(*) AS count FROM " + location + " WHERE " + whereStatement;

		try {
			// The parameter currently being checked for
			//
			// While paramNum is 1-based, its access is not
			//
			// Although it is not actively shown, this is used to stay with
			// PostgreSQL's 1-based standard on parameters in prepared statements
			int paramNum = 1;
			
			// Whether or not we're in a `LIKE` statement
			boolean inLike = false;
			
			// The new query to parse the original query into
			String newQuery = "";

			for (int i = 0; i < query.length(); i++) {
				
				// If we're in a `LIKE` statement, we want to switch the boolean value and step the iterator forward 4 steps
				if (i < query.length() - 4)
					if (query.subSequence(i, i + 4).toString().equals("LIKE")) {
						newQuery += "LIKE ";
						
						i += 4;
						
						inLike = true;
					}
				
				// If we're in a `LIKE` statement, get which kind it is from the token (see the `like` method's description
				// for an explanation of the three values.
				//
				// We need to manually alter the value of the object here.
				if (inLike) {
					
					// Prefix
					if (query.charAt(i) == '1') {
						newQuery += "?";
						whereObjects[paramNum - 1] = "%" + (String)whereObjects[paramNum - 1];
						i++;
						inLike = false;
					}
					
					// Postfix
					else if (query.charAt(i) == '2') {
						newQuery += "?";
						whereObjects[paramNum - 1] = (String)whereObjects[paramNum - 1] + "%";
						i++;
						inLike = false;
					}
					
					// Anywhere (prefix AND postfix)
					else if (query.charAt(i) == '3') {
						newQuery += "?";
						whereObjects[paramNum - 1] = "%" + (String)whereObjects[paramNum - 1] + "%";
						i++;
						inLike = false;
					}
				}
				
				else {
					newQuery += query.charAt(i);
				}
				
				if (query.charAt(i) == '?')
					paramNum++;
			}

			Connection        conn = DriverManager.getConnection(url, username, password);
			PreparedStatement stmt = conn.prepareStatement(newQuery);

			// Iterate through all of the objects for the `WHERE` statement to add them into the prepared statement
			for (int i = 0; i < whereObjects.length; i++) {
				stmt.setObject(i + 1, whereObjects[i]);
			}

			// Get the result and its metadata
			ResultSet         res 	   = stmt.executeQuery();
			ResultSetMetaData metaData = res.getMetaData();
			
			// Iterate the result set to get the number
			// as it's placed before the first row.
			res.next();
			
			// Get the count before closing the statement, as
			// closing the Statement closes all ResultSet objects
			int count = res.getInt("count");
			
			stmt.close();
			conn.close();
			
			return count;
		}
		
		catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return 0;
	}
}
