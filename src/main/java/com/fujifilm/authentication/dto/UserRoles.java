package com.fujifilm.authentication.dto;

import java.math.BigDecimal;

import com.fujifilm.annotation.Column;
import com.fujifilm.annotation.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRoles {
	@Column(name = "userId")
	private BigDecimal userId;
	@Column(name = "roleId")
	private BigDecimal roleId;
}
