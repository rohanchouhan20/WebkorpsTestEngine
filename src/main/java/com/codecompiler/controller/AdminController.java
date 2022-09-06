package com.codecompiler.controller;


import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.codecompiler.entity.Admin;
import com.codecompiler.entity.User;
import com.codecompiler.exception.UserAlreadyExistException;
import com.codecompiler.service.AdminService;
import com.codecompiler.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

import com.codecompiler.reponse.ResponseHandler;

@Controller
@CrossOrigin(origins = "*")
@Slf4j
public class AdminController {

	@Autowired
	private AdminService adminService;
	
	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private AuthenticationManager authenticationManager;

	@PostMapping("public/admin/signIn")
	public ResponseEntity<Object> doLogin(@RequestBody User user) {
		log.info("doLogin started user email ::"+user.getEmail());
		Authentication authObj;
		try {
			authObj = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail().toLowerCase(), user.getPassword()));
		} catch (BadCredentialsException e) {
			log.error("Exception occured in doLogin :: "+e.getMessage());
			return ResponseHandler.generateResponse("failer", HttpStatus.UNAUTHORIZED, "email and password does not match");
		}
		String token = jwtUtil.generateToken(authObj.getName());
		log.info("doLogin ended token generated successfully");
		return ResponseHandler.generateResponse("success", HttpStatus.OK, token);
	}

	@PostMapping("public/adminRegistration")
	private ResponseEntity<Object> adminRegistration(@RequestBody Admin admin) {
		log.info("adminRegistration started admin email ::"+admin.getEmail());
		try {
				adminService.saveAdminDetails(admin);
				log.info("Admin details saved successfully");
			
		} catch (UserAlreadyExistException e) 
		{
			log.error("Exception occured in adminRegistration :: "+e.getMessage());
			return ResponseHandler.generateResponse("error", HttpStatus.CONFLICT, e.getMessage());
		}
		catch (Exception e) {
			log.error("Exception occured in adminRegistration :: "+e.getMessage());
			return ResponseHandler.generateResponse("error", HttpStatus.INTERNAL_SERVER_ERROR, "Admin registration failer");
		}
		return ResponseHandler.generateResponse("success", HttpStatus.OK, "Admin registered successfully");
	}

}
