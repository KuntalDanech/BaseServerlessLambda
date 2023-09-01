package com.fujifilm.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fujifilm.authentication.dto.Permission;
import com.fujifilm.authentication.dto.Roles;
import com.fujifilm.authentication.dto.User;
import com.fujifilm.authentication.dto.UserRoles;
import com.fujifilm.exception.UnAuthorizedException;
import com.fujifilm.exception.BadRequestException;
import com.fujifilm.exception.BusinessException;
import com.fujifilm.util.ResultSetMapper;

public abstract class APIAbstractService
		implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	protected static Logger log = Logger.getLogger(APIAbstractService.class.getName());

	protected static ObjectMapper mapper = new ObjectMapper();

	protected static Connection connection;

	protected static Map<String, Object> response;

	public static Connection getConnection() {
		try {
			connection = DriverManager.getConnection(
					"JDBC URL",
					"Username", "Password");
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return connection;
	}

	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		response = new LinkedHashMap<>();
		getConnection();
		if (connection != null) {
			log.info("We got the connection");
		}
		try {
			log.info("request query param: " + input.getQueryStringParameters());
			log.info("request path param: " + input.getPathParameters());
			log.info("request body: " + input.getBody());
			log.info("request headers: " + input.getHeaders());
			String token = getAccessToken(input.getHeaders());
			log.info("token: " + token);
			Object response = handle(input, context, token);
			log.info("response body: " + response);
			try {
				return new APIGatewayProxyResponseEvent().withBody(mapper.writeValueAsString(response)).withStatusCode(200);
			} catch (JsonProcessingException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
				return new APIGatewayProxyResponseEvent()
						.withBody(Map.of("status", false, "message", e.getMessage()).toString()).withStatusCode(500);
			}
		} catch (BusinessException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return new APIGatewayProxyResponseEvent()
					.withBody(Map.of("status", false, "message", ex.getMessage()).toString()).withStatusCode(500);
		} catch (BadRequestException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return new APIGatewayProxyResponseEvent()
					.withBody(Map.of("status", false, "message", ex.getMessage()).toString()).withStatusCode(400);
		} catch (UnAuthorizedException ex) {
			log.log(Level.SEVERE, ex.getMessage(), ex);
			return new APIGatewayProxyResponseEvent()
					.withBody(Map.of("status", false, "message", ex.getMessage()).toString()).withStatusCode(401);
		} finally {
			if (connection != null) {
				log.info("Closed the connection");
				try {
					connection.close();
				} catch (SQLException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

	protected String getAccessToken(Map<String, String> headers) {
		return headers.getOrDefault("Authorization", null);
	}

	protected abstract Object handle(APIGatewayProxyRequestEvent input, Context context, String token)
			throws BusinessException;

	public static void main(String[] args) {
		getConnection();
		try (PreparedStatement ps = connection
				.prepareStatement("select * from fbaucustportal.usr u where  u.username = 'kuntal.danech.uw@fujifilm.com' limit 10")) {
			ResultSet rs = ps.executeQuery();
			List<User> accountList = ResultSetMapper.mapRersultSetToListOfObject(rs, User.class);
			for (User user : accountList) {
				List<Roles> userRolesList = new ArrayList<>();
				try (PreparedStatement ps1 = connection
						.prepareStatement("select  * from fbaucustportal.user_roles_v2 urv where urv.\"userId\" = ?")) {
					ps1.setLong(1, user.getId().longValue());
					List<UserRoles> userRoles = ResultSetMapper.mapRersultSetToListOfObject(ps1.executeQuery(),
							UserRoles.class);
					for (UserRoles userRole : userRoles) {
						System.out.println(userRole);
						try (PreparedStatement ps2 = connection.prepareStatement("""
									select * from fbaucustportal.roles_v2 rv where rv."roleId" = ?
								""")) {
							ps2.setLong(1, userRole.getRoleId().longValue());
							List<Roles> roles = ResultSetMapper.mapRersultSetToListOfObject(ps2.executeQuery(),
									Roles.class);
							for (Roles role : roles) {
								System.out.println(role);
								try (PreparedStatement ps3 = connection.prepareStatement("""
											select * from fbaucustportal.permissions p where p.role_id = ?
										""")) {
									ps3.setLong(1, role.getId().longValue());
									List<Permission> permissions = ResultSetMapper.mapRersultSetToListOfObject(ps3.executeQuery(),
											Permission.class);
									role.setPermissions(permissions);
								}
								userRolesList.add(role);
							}

						}
					}
				}
				user.setRoles(userRolesList);
				log.info("User : "+user.getRoles());
			}
		} catch (Exception e1) {
			log.log(Level.SEVERE, e1.getMessage(), e1);
			throw new BusinessException("Internal server error");
		}
	}
}
