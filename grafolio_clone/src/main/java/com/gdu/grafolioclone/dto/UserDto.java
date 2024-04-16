package com.gdu.grafolioclone.dto;

import java.sql.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserDto {
	private int userNo, signupKind;
	private String email, pw, name, mobile;
	private Date pwModifyDt, signupDt;
}
