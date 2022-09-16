package com.codecompiler.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.codecompiler.dto.CodeDetailsDTO;
import com.codecompiler.dto.CodeResponseDTO;
import com.codecompiler.entity.Contest;
import com.codecompiler.entity.Student;
import com.codecompiler.entity.TestCases;
import com.codecompiler.service.CodeProcessingService;
import com.codecompiler.service.ContestService;
import com.codecompiler.service.QuestionService;
import com.codecompiler.service.StudentService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CodeProcessingServiceImpl implements CodeProcessingService {

	@Autowired
	private StudentService studentService;

	@Autowired
	private QuestionService questionService;

	@Autowired
	private ContestService contestService;

	private static String compilationCommand(String language) {
		String command = null;
		if (language.equalsIgnoreCase("java")) {
			command = "javac Main.java";
		} else if (language.equalsIgnoreCase("python")) {
			command = "src/main/resources/temp/HelloPython.py";
		} else if (language.equalsIgnoreCase("cpp")) {
			command = "g++ HelloCPP.cpp -o exeofCPP";
		} else if (language.equalsIgnoreCase("c")) {
			command = "gcc HelloC.c -o exeofc";
		}
		return command;
	}

	private static String interpretationCommand(String language) {
		String command = null;
		if (language.equalsIgnoreCase("java")) {
			command = "java Main ";
		} else if (language.equalsIgnoreCase("python")) {
			command = "py HelloPython.py ";
		} else if (language.equalsIgnoreCase("cpp")) {
			command = "src/main/resources/temp/" + "exeofCPP ";
		} else if (language.equalsIgnoreCase("c")) {
			command = "src/main/resources/temp/exeofc ";
		}
		return command;
	}

	private void saveCodeTemporary(String code, String language) throws IOException {
		log.info("saveCodeTemporary: started");
		FileWriter fl = null;
		if (language.equalsIgnoreCase("java")) {
			String fileNameInLocal = "Main.java";
			fl = new FileWriter("src/main/resources/temp/" + fileNameInLocal);
		} else if (language.equalsIgnoreCase("python")) {
			String fileNameInLocal = "HelloPython";
			fl = new FileWriter("src/main/resources/temp/" + fileNameInLocal + "." + "py");
		} else if (language.equalsIgnoreCase("cpp")) {
			String fileNameInLocal = "HelloCPP";
			fl = new FileWriter("src/main/resources/temp/" + fileNameInLocal + "." + "cpp");
		} else if (language.equalsIgnoreCase("c")) {
			String fileNameInLocal = "HelloC";
			fl = new FileWriter("src/main/resources/temp/" + fileNameInLocal + "." + "c");
		}
		PrintWriter pr = new PrintWriter(fl);
		pr.write(code);
		pr.flush();
		pr.close();
		log.info("saveCodeTemporary: ended");
	}

	private static String getMessagesFromProcessInputStream(InputStream inputStream) throws IOException {
		log.info("getMessagesFromProcessInputStream :: started");
		String message = "";
		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		String line = null;
		while ((line = in.readLine()) != null) {
			message += line + "\n";
		}
		log.info("getMessagesFromProcessInputStream :: end");
		return message;
	}

	private CodeResponseDTO saveSubmittedCode(CodeDetailsDTO codeDetailsDTO, ArrayList<Boolean> testCasesSuccess,
			String complilationMessage) throws IOException {
		log.info("saveSubmittedCode :: started");
		String submittedCodeFileName = codeDetailsDTO.getQuestionId() + "_" + codeDetailsDTO.getStudentId();
		Student student = studentService.findById(codeDetailsDTO.getStudentId());
		Set<String> studentQuestionIds = student.getQuestionId();
		log.info("student question ids::" + studentQuestionIds);
		if (studentQuestionIds == null) {
			studentQuestionIds = new HashSet<>();
		}
		studentQuestionIds.add(codeDetailsDTO.getQuestionId());
		CodeResponseDTO codeResponseDTO = new CodeResponseDTO();

		FileWriter flSubmitted = new FileWriter("src/main/resources/CodeSubmittedByCandidate/" + submittedCodeFileName);
		PrintWriter prSubmitted = new PrintWriter(flSubmitted);
		prSubmitted.write(codeDetailsDTO.getCode());
		studentService.updateStudentDetails(codeDetailsDTO.getStudentId(), codeDetailsDTO.getContestId(),
				studentQuestionIds, testCasesSuccess, complilationMessage, submittedCodeFileName);
		prSubmitted.flush();
		prSubmitted.close();
		codeResponseDTO.setTestCasesSuccess(testCasesSuccess);
		codeResponseDTO.setSuccessMessage("Code Submitted Successfully");
		log.info("saveSubmittedCode ::ended & Code Submitted Successfully");
		return codeResponseDTO;
	}

	@Override
	public CodeResponseDTO compileCode(CodeDetailsDTO codeDetailsDTO) throws IOException {
		log.info("compile code: started");
		String language = codeDetailsDTO.getLanguage();
		String questionId = codeDetailsDTO.getQuestionId();
		int flag = codeDetailsDTO.getFlag();
		CodeResponseDTO codeResponseDTO = new CodeResponseDTO();
		ArrayList<Boolean> testCasesSuccess = new ArrayList<Boolean>();
		Process pro = null;
		saveCodeTemporary(codeDetailsDTO.getCode(), language);
		try {
			String compilationCommand = compilationCommand(language);
			pro = Runtime.getRuntime().exec(compilationCommand, null, new File("src/main/resources/temp/"));
			String complilationMessage = getMessagesFromProcessInputStream(pro.getErrorStream());
			if (!complilationMessage.isEmpty() && flag == 0) {
				codeResponseDTO.setComplilationMessage(complilationMessage);
				log.info("compile code :: compilation error :: " + complilationMessage);
				return codeResponseDTO;
			}
			String interpretationCommand = interpretationCommand(language);
			pro = Runtime.getRuntime().exec(interpretationCommand, null, new File("src/main/resources/temp/"));
			String exceptionMessage = getMessagesFromProcessInputStream(pro.getErrorStream());
			if (!exceptionMessage.isEmpty() && flag == 0) {
				codeResponseDTO.setComplilationMessage(exceptionMessage);
				log.info("compile code :: exception occured :: " + exceptionMessage);
				return codeResponseDTO;
			}
			List<TestCases> testCases = questionService.getTestCase(questionId);
			for (TestCases testCase : testCases) {
				String input = testCase.getInput();
				pro = Runtime.getRuntime().exec(interpretationCommand + input, null,
						new File("src/main/resources/temp/"));
				String interpretationMessage = getMessagesFromProcessInputStream(pro.getInputStream());
				interpretationMessage = interpretationMessage.substring(0, interpretationMessage.length() - 1);
				if (interpretationMessage.contains(testCase.getOutput())
						|| interpretationMessage.equals(testCase.getOutput())) {
					testCasesSuccess.add(true);
				} else {
					testCasesSuccess.add(false);
				}
			}
			/*
			 * Contest contestDetails =
			 * contestService.findByContestId(codeDetailsDTO.getContestId()); &&
			 * (codeDetailsDTO.getTimeOut() || contestDetails.getQuestionStatus()
			 * .get(contestDetails.getQuestionStatus().size() -
			 * 1).getQuestionId().equals(questionId))
			 */
			if (flag == 1) {
				return saveSubmittedCode(codeDetailsDTO, testCasesSuccess, complilationMessage);
			}
			codeResponseDTO.setTestCasesSuccess(testCasesSuccess);
		} catch (IOException e) {
			codeResponseDTO.setComplilationMessage(e.getMessage());
			log.error("Object is null " + e.getMessage());
		} catch (Exception e) {
			log.error("Object is null " + e.getMessage());
			codeResponseDTO.setComplilationMessage("Something wents wrong. Please contact to HR");
		}

		log.info("compile code: ended");

		return codeResponseDTO;
	}
}
