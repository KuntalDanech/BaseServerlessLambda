package com.fujifilm.authentication.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fujifilm.annotation.Column;
import com.fujifilm.annotation.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
	@Column(name = "id")
	private BigDecimal id;

	@Column(name = "active")
	private BigDecimal active;

	@Column(name = "type")
	private String type;

	@Column(name = "title")
	private String title;

	@Column(name = "username")
	private String userName;

	@Column(name = "emailaddr")
	private String emailAddress;

	@Column(name = "password")
	private String password;

	@Column(name = "password_salt")
	private String passwordSalt;

	@Column(name = "firstname")
	private String firstName;

	@Column(name = "lastname")
	private String lastName;

	@Column(name = "dayphone")
	private String contactNumber;

	@Column(name = "company")
	private String company;

	@Column(name = "account_locked_flag")
	private String accountLockedFlag;

	@Column(name = "account_locked_date")
	private Date accountLockedDate;

	@Column(name = "password_force_change_flag")
	private String passwordForceChangeFlag;

	@Column(name = "password_expires_flag")
	private String passwordExpiryFlag;

	@Column(name = "password_expiry_Date")
	private Date passwordExpiryDate;

	@Column(name = "unsuccessful_login_attempts")
	private Integer unsuccessfulLoginAttempts;

	@Column(name = "PASSWORD_LAST_CHANGED_DATE")
	Date passwordLastChangedDate;

	@Column(name = "date_reg")
	private String dateReg;

	@Column(name = "jobtitle")
	private String jobtitle;

	@Column(name = "mobilephone")
	private String mobilephone;

	@Column(name = "remarks")
	private String remarks;

	@Column(name = "action_required")
	private String actionRequired;

	@Column(name = "email_verified")
	private String emailVerified;

	@Column(name = "block_elk_extract_flag")
	private String blockELKExtract;

	@Column(name = "registered_by")
	private String registeredBy;

	@Column(name = "date_mod")
	private String dateMod;
	
	private List<Roles> roles;

}
