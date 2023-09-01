package com.fujifilm.authentication.dto;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDetailsResponse {
	private Boolean status;
	private Integer code;
	private String token;
	private List<Roles> userRoles;
	private String userName;
	private String emailAddress;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String company;
	private String title;
	private String mobileNumber;
	private String jobTitle;
	private String areaCode;
	private String message;
	private Long active;
	private String accountLockedFlag;
	private boolean mustChangePassword;
	private boolean showNewChangesPopup;
	private String lastLoggedInDate;
	private Long userId;
}