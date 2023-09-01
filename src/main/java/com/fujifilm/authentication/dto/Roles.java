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
public class Roles {
	@Column(name = "roleId")
	private BigDecimal id;
	
	@Column(name = "roleName")
	private String roleName;
	
	@Column(name = "active")
	private Boolean active;
	
	@Column(name = "createdDate")
	private Date createdDate;
	
	@Column(name = "updatedDate")
	private Date updatedDate;
	
	private List<Permission> permissions;	
}
