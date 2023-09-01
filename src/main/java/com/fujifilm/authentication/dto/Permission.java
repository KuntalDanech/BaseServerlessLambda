package com.fujifilm.authentication.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fujifilm.annotation.Column;
import com.fujifilm.annotation.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Permission {
	@Column(name = "id")
	private Integer id;

	@Column(name = "name")
	private String name;

	@Column(name = "active")
	private Boolean active;

	@Column(name = "createdDate")
	private Date createdDate;

	@Column(name = "updatedDate")
	private Date updatedDate;

	@Column(name = "updatedDate")
	private BigDecimal role_id;

}
