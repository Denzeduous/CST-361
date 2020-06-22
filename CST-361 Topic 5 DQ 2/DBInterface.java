package service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javax.ejb.Local;

import model.User;

@Local
public interface DBInterface {
	public String createLocation(String schema, String table);
	
	public String set   	  (String[] names);
	public String where		  (String   name);
	public String conditionals(String[] statements);
	public String like	      (String   fieldName, int type);
	
	public HashMap<String, Object> select(String location, String whereStatement, Object[] whereObjects);
	public HashMap<String, Object> select(String location, String whereStatement, Object whereObject);
	
	public ArrayList<HashMap<String, Object>> selectMany(String location, String whereStatement, Object[] whereObjects);
	public ArrayList<HashMap<String, Object>> selectMany(String location, String whereStatement, Object value);
	
	public ArrayList<HashMap<String, Object>> selectAll (String location);
	
	public boolean authenticate(User user);
	
	public boolean delete         (String location, String   whereStatement, Object[] whereObjects);
	public boolean insert         (String location, String[] fieldNames,     Object[] values);
	public Object  insertAndReturn(String location, String[] fieldNames,     Object[] values,     String columnToReturn);
	public boolean update         (String location, String   setStatement,   Object[] setObjects, String whereStatement, Object[] whereObjects);
	public int     count          (String location, String   whereStatement, Object[] whereObjects);
	
	public boolean  usernameExists   (String username);
	public boolean  emailExists      (String email);
	public int 	    insertUser       (User 	 user);
	public User 	getUserbyID      (UUID 	 userID);
	public User 	getUserbyUsername(String username);
}
