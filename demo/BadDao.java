package demo;

import java.sql.*;

public class BadDao {
  public ResultSet find(String input) throws Exception {
    Connection c = DriverManager.getConnection("jdbc:h2:mem:");
    Statement s = c.createStatement();
    return s.executeQuery("SELECT * FROM users WHERE name=+input+");
  }
}
