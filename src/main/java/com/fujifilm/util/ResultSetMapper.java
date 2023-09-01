package com.fujifilm.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.fujifilm.annotation.Column;
import com.fujifilm.annotation.Entity;

public class ResultSetMapper {
	private ResultSetMapper() {}
	// This method is already implemented in package
	// but as far as I know it accepts only public class attributes
	private static void setProperty(Object clazz, String fieldName, Object columnValue) {
		try {
			// get all fields of the class (including public/protected/private)
			Field field = clazz.getClass().getDeclaredField(fieldName);
			// this is necessary in case the field visibility is set at private
			field.setAccessible(true);
			field.set(clazz, columnValue);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static <T> List<T> mapRersultSetToListOfObject(ResultSet rs, Class<T> clazz) {
		List<T> outputList = new ArrayList<>();
		try {
			// make sure resultset is not null
			if (rs != null) {

				// check if Class clazz has the 'Entity' annotation
				if (clazz.isAnnotationPresent(Entity.class)) {

					// get the resultset metadata
					ResultSetMetaData rsmd = rs.getMetaData();

					// get all the attributes of Class clazz
					Field[] fields = clazz.getDeclaredFields();

					T bean = null;
					while (rs.next()) {
						try {
							bean = (T) clazz.getConstructor().newInstance();
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						} catch (SecurityException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
						for (int _iterator = 0; _iterator < rsmd.getColumnCount(); _iterator++) {
							// get the SQL column name
							String columnName = rsmd.getColumnName(_iterator + 1);

							// get the value of the SQL column
							Object columnValue = rs.getObject(_iterator + 1);

							// iterating over clazz attributes to check
							// if any attribute has 'Column' annotation with matching 'name' value
							for (Field field : fields) {
								if (field.isAnnotationPresent(Column.class)) {
									Column column = field.getAnnotation(Column.class);
									if (column.name().equalsIgnoreCase(columnName) && columnValue != null) {
										setProperty(bean, field.getName(), columnValue);
										break;
									}
								}
							} // EndOf for(Field field : fields)
						} // EndOf for(_iterator...)
						if (outputList == null) {
							outputList = new ArrayList<T>();
						}
						outputList.add(bean);
					} // EndOf while(rs.next())
				} else {
					// throw some error that Class clazz
					// does not have @Entity annotation
				}
			} else {
				// ResultSet is empty
				return null;
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		return outputList;
	}

	public static <T> T mapRersultSetToObject(ResultSet rs, Class<T> clazz) {
		T bean = null;
		try {
			// make sure resultset is not null
			if (rs != null) {

				// check if Class clazz has the 'Entity' annotation
				if (clazz.isAnnotationPresent(Entity.class)) {

					// get the resultset metadata
					ResultSetMetaData rsmd = rs.getMetaData();

					// get all the attributes of Class clazz
					Field[] fields = clazz.getDeclaredFields();

					while (rs.next()) {
						try {
							bean = (T) clazz.getConstructor().newInstance();
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						} catch (SecurityException e) {
							e.printStackTrace();
						} catch (NoSuchMethodException e) {
							e.printStackTrace();
						}
						for (int _iterator = 0; _iterator < rsmd.getColumnCount(); _iterator++) {
							// get the SQL column name
							String columnName = rsmd.getColumnName(_iterator + 1);

							// get the value of the SQL column
							Object columnValue = rs.getObject(_iterator + 1);

							// iterating over clazz attributes to check
							// if any attribute has 'Column' annotation with matching 'name' value
							for (Field field : fields) {
								if (field.isAnnotationPresent(Column.class)) {
									Column column = field.getAnnotation(Column.class);
									if (column.name().equalsIgnoreCase(columnName) && columnValue != null) {
										setProperty(bean, field.getName(), columnValue);
										break;
									}
								}
							} // EndOf for(Field field : fields)
						} // EndOf for(_iterator...)
						return bean;
					} // EndOf while(rs.next())
				} else {
					// throw some error that Class clazz
					// does not have @Entity annotation
				}
			} else {
				// ResultSet is empty
				return null;
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
		return null;
	}
}
