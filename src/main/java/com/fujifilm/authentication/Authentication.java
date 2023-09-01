package com.fujifilm.authentication;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fujifilm.authentication.dto.Permission;
import com.fujifilm.authentication.dto.Roles;
import com.fujifilm.authentication.dto.User;
import com.fujifilm.authentication.dto.UserDetailsResponse;
import com.fujifilm.authentication.dto.UserRoles;
import com.fujifilm.base.APIAbstractService;
import com.fujifilm.exception.UnAuthorizedException;
import com.fujifilm.exception.BadRequestException;
import com.fujifilm.exception.BusinessException;
import com.fujifilm.util.JWTUtil;
import com.fujifilm.util.ResultSetMapper;

public class Authentication extends APIAbstractService {

	private static final Random RANDOM = new SecureRandom();

	@Override
	protected Object handle(APIGatewayProxyRequestEvent input, Context context, String token)
			throws BusinessException, BadRequestException {

		final String method = input.getHttpMethod();
		final String path = input.getPath();
		final String body = input.getBody();
		switch (method) {
		case "POST":
			if (path.equalsIgnoreCase("/token")) {
				User user = authenticateUser(body);
				validateUserStatus(user);
				resetUnsuccessfulCount(user);
				UserDetailsResponse detailsResponse = buildUserDetailsResponse(user);
				insertCode(detailsResponse);
				response.put("token", generateToken(user));
				response.put("status", "message");
				response.put("message", "success");
				response.put("code", 200);
				return response;
			} else if (path.equalsIgnoreCase("/code/validate")) {
				String userName = getUserFromToken(token);
				validateCode();
				return getResponse(userName);
			}
			break;
		}
		throw new BadRequestException("Request could not be served");
	}

	public User authenticateUser(String requestBody) {
		JsonNode node;
		User user = null;
		try {
			node = mapper.readTree(requestBody);
		} catch (JsonProcessingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BadRequestException("Invalid request body");
		}
		String userName = node.get("userName").asText();
		String password = node.get("password").asText();
		if (userName == null || password == null)
			throw new BadRequestException("Username and password is required");
		if (userName.isBlank() || password.isBlank())
			throw new BadRequestException("Username and password is required");
		userName = userName.trim();
		try (PreparedStatement countPs = connection.prepareStatement(
				"select count(*) as count from fbaucustportal.usr u where u.username =? and u.active=1")) {
			countPs.setString(1, userName);
			ResultSet rsCount = countPs.executeQuery();
			while (rsCount.next()) {
				if (rsCount.getInt("count") == 0) {
					throw new UnAuthorizedException("Unauthorized user");
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BusinessException("Internal server error");
		}
		try (PreparedStatement ps = connection
				.prepareStatement("select * from fbaucustportal.usr u where u.username =? and u.active=1 limit 1")) {
			ps.setString(1, userName);
			ResultSet rs = ps.executeQuery();
			user = ResultSetMapper.mapRersultSetToObject(rs, User.class);
			String saltKey = user.getPasswordSalt();
			byte[] e = Base64.getDecoder().decode(saltKey);
			var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			var spec = new PBEKeySpec(password.toCharArray(), e, 10000, 160);
			SecretKey key = skf.generateSecret(spec);
			byte[] res = key.getEncoded();
			String passwordEncoded = Base64.getEncoder().encodeToString(res);
			if (!passwordEncoded.equals(user.getPassword())) {
				throw new UnAuthorizedException("Unauthorized user");
			}
			return user;
		} catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e1) {
			log.log(Level.SEVERE, e1.getMessage(), e1);
			throw new BusinessException("Internal server error");
		}
	}

	private String generateToken(User user) {
		var expiry = Calendar.getInstance();
		expiry.add(Calendar.MINUTE, +30);
		return JWTUtil.generateToken(user.getEmailAddress().toLowerCase().trim(), "FBAU-SALES-PORTAL_1.0",
				"FBAU_SALES_PORTAL_7398", expiry);
	}

	private void validateUserStatus(User user) {
		if (Boolean.TRUE.toString().equalsIgnoreCase(user.getActionRequired())) {
			throw new UnAuthorizedException(
					"Sorry can't login now, Our Support team will contact you as your details need to be verified.");
		} else if (Boolean.FALSE.toString().equalsIgnoreCase(user.getEmailVerified())) {
			throw new UnAuthorizedException(
					"Your email address needs to be verified and activated, please check verification email in your registered email Inbox, spam or junk folder.");
		} else if (user.getActive().longValue() != 1L) {
			throw new UnAuthorizedException("Your account is not active, please reset your password and login.");
		}
	}

	private void resetUnsuccessfulCount(User user) {
		try (PreparedStatement ps = connection.prepareStatement("""
				UPDATE fbaucustportal.usr
				SET unsuccessful_login_attempts=0, account_locked_flag='N'
				WHERE id=?;
				""")) {
			ps.setLong(1, user.getId().longValue());
			if (ps.executeUpdate() > 0) {
				log.log(Level.INFO, "Reset unsuccessful count");
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BusinessException("Internal server error");
		}
	}

	private UserDetailsResponse buildUserDetailsResponse(User user) {
		UserDetailsResponse jwtResponse = new UserDetailsResponse();
		jwtResponse.setFirstName(user.getFirstName());
		jwtResponse.setUserId(user.getId().longValue());
		jwtResponse.setLastName(user.getLastName());
		if (user.getContactNumber() != null) {
			jwtResponse.setPhoneNumber(user.getContactNumber());
		} else {
			jwtResponse.setPhoneNumber("");
		}
		jwtResponse.setCompany(user.getCompany());
		jwtResponse.setTitle(user.getTitle());
		jwtResponse.setMobileNumber(user.getMobilephone());
		jwtResponse.setJobTitle(user.getJobtitle());
		jwtResponse.setEmailAddress(user.getEmailAddress());
		var expiry = Calendar.getInstance();
		expiry.add(Calendar.HOUR_OF_DAY, +1);
		jwtResponse.setToken(JWTUtil.generateToken(user.getEmailAddress().toLowerCase().trim(), "FBAU-SALES-PORTAL_1.0",
				"FBAU_SALES_PORTAL_7398", expiry));
		var mustChangePassword = isPasswordForceChangeFlag(user) || isUserPasswordExpired(user);
		jwtResponse.setMustChangePassword(mustChangePassword);
		jwtResponse.setUserName(user.getFirstName() + " " + user.getLastName());
		jwtResponse.setUserRoles(getRoles(user));
		jwtResponse.setCode(200);
		jwtResponse.setStatus(Boolean.TRUE);
		jwtResponse.setMessage("Success");
		return jwtResponse;
	}

	private boolean isPasswordForceChangeFlag(User user) {
		return user != null && user.getPasswordForceChangeFlag() != null
				&& "Y".equalsIgnoreCase(user.getPasswordForceChangeFlag());
	}

	private boolean isUserPasswordExpired(User account) {
		var expired = false;
		Date expiryDate;
		if (account.getPasswordExpiryFlag() != null && "Y".equalsIgnoreCase(account.getPasswordExpiryFlag())) {
			expiryDate = account.getPasswordExpiryDate();
		} else {
			expiryDate = null;
		}

		if (expiryDate != null && (new Date()).compareTo(expiryDate) > 0) {
			expired = true;
		}
		log.log(Level.INFO, "isUserPasswordExpired: {0}", expired);
		return expired;
	}

	private List<Roles> getRoles(User user) {

		List<Roles> userRolesList = new ArrayList<>();
		try (PreparedStatement ps1 = connection
				.prepareStatement("select  * from fbaucustportal.user_roles_v2 urv where urv.\"userId\" = ?")) {
			ps1.setLong(1, user.getId().longValue());
			List<UserRoles> userRoles = ResultSetMapper.mapRersultSetToListOfObject(ps1.executeQuery(),
					UserRoles.class);
			for (UserRoles userRole : userRoles) {
				try (PreparedStatement ps2 = connection.prepareStatement("""
							select * from fbaucustportal.roles_v2 rv where rv."roleId" = ?
						""")) {
					ps2.setLong(1, userRole.getRoleId().longValue());
					List<Roles> roles = ResultSetMapper.mapRersultSetToListOfObject(ps2.executeQuery(), Roles.class);
					for (Roles role : roles) {
						try (PreparedStatement ps3 = connection.prepareStatement("""
									select * from fbaucustportal.permissions p where p.role_id = ?
								""")) {
							ps3.setLong(1, role.getId().longValue());
							List<Permission> permissions = ResultSetMapper
									.mapRersultSetToListOfObject(ps3.executeQuery(), Permission.class);
							role.setPermissions(permissions);
						} catch (SQLException e) {
							log.log(Level.SEVERE, e.getMessage(), e);
							throw new BusinessException("Internal server error");
						}
						userRolesList.add(role);
					}
				} catch (SQLException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					throw new BusinessException("Internal server error");
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BusinessException("Internal server error");
		}
		return userRolesList;
	}

	private void insertCode(UserDetailsResponse detailsResponse) {
		try (PreparedStatement ps = connection.prepareStatement("""
				INSERT INTO fbaucustportal.code_verification
				(username, token_json, code, createddatetime, code_salt)
				VALUES(?,?,?,?,?)
				ON CONFLICT (username)
				DO
				UPDATE SET token_json = ?, code= ?, createddatetime= ?, code_salt = ?;
				""")) {
			ps.setString(1, detailsResponse.getEmailAddress());
			ps.setString(2, mapper.writeValueAsString(detailsResponse));
			var code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
			var salt = new byte[8];
			RANDOM.nextBytes(salt);
			var saltKey = Base64.getEncoder().encodeToString(salt);
			var encodedCode = hashCodeWithSalt(code, saltKey);
			ps.setString(3, encodedCode);
			ps.setDate(4, new java.sql.Date(System.currentTimeMillis()));
			ps.setString(5, saltKey);

			ps.setString(6, mapper.writeValueAsString(detailsResponse));
			ps.setString(7, encodedCode);
			ps.setDate(8, new java.sql.Date(System.currentTimeMillis()));
			ps.setString(9, saltKey);
			if (ps.executeUpdate() > 0) {
				log.info("Code has been geneated");
			} else {
				log.info("Code has not been generated, something went wrong");
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BusinessException("Internal server error");
		} catch (JsonProcessingException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BusinessException("Internal server error");
		}

	}

	public String hashCodeWithSalt(String cleartextCode, String salt) {
		try {
			byte[] e = Base64.getDecoder().decode(salt);
			var skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			var spec = new PBEKeySpec(cleartextCode.toCharArray(), e, 10000, 160);
			SecretKey key = skf.generateSecret(spec);
			byte[] res = key.getEncoded();
			return Base64.getEncoder().encodeToString(res);
		} catch (Exception var8) {
			var8.printStackTrace();
			class HashCodeException extends RuntimeException {

				private static final long serialVersionUID = 1L;

				public HashCodeException(String msg) {
					super(msg);
				}
			}
			throw new HashCodeException(var8.getMessage());
		}
	}

	private void validateCode() {
		// TODO : By pass at the moment
	}

	private String getUserFromToken(String token) {
		try {
			return JWTUtil.getUserName(token, "FBAU_SALES_PORTAL_7398");
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new UnAuthorizedException("UnAuthorized User");
		}
	}

	private UserDetailsResponse getResponse(String userName) {
		try (PreparedStatement ps = connection.prepareStatement("""
				select * from fbaucustportal.code_verification cv where cv.username = ?
				""")) {
			ps.setString(1, userName);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				try {
					return mapper.readValue(rs.getString("token_json"), UserDetailsResponse.class);
				} catch (JsonProcessingException e) {
					log.log(Level.SEVERE, e.getMessage(), e);
					throw new BusinessException("Internal server error");
				}
			}
			throw new UnAuthorizedException("UnAuthorized User");
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new BusinessException("Internal server error");
		}
	}

	public static void main(String[] args) {
//		Authentication au = new Authentication();
//		au.getConnection();
//		User user = au.authenticateUser("""
//				{
//					"userName":"kuntal.danech.uw@fujifilm.com",
//					"password":"Uat*2023"
//				}
//				""");
//		au.validateUserStatus(user);
//		au.resetUnsuccessfulCount(user);
//		UserDetailsResponse detailsResponse = au.buildUserDetailsResponse(user);
//		au.insertCode(detailsResponse);
//		try {
//			System.out.println(mapper.writeValueAsString(detailsResponse));
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

//		Authentication au = new Authentication();
//		au.getConnection();
//		String str = au.getResponse("kuntal.danech.uw@fujif");
//		System.out.println(str);

	}
}