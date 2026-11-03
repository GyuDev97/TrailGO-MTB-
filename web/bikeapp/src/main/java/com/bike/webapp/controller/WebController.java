package com.bike.webapp.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
@RequestMapping("/")
@Controller
public class WebController {
	
	//index2 > index로 변경시 지워도됨
	@RequestMapping("/testf/{filename}")
	public String tttt(@PathVariable("filename") String filename, Model model) {
		System.out.println(filename);
		return null;
	}
	
	@RequestMapping("/")
	public String main() {
		return "forward:/index.html";
	}
	
	@RequestMapping("/index")
	public String index() {
		return "redirect:/";
	}

	@RequestMapping("/privacy")
	public String privacy() {
		return "forward:/privacy.html";
	}

	@RequestMapping("/terms")
	public String terms() {
		return "forward:/terms.html";
	}

	@RequestMapping("/support")
	public String support() {
		return "forward:/support.html";
	}
	@RequestMapping("/intro")
	public String intro() {
		return "/intro";
	}
	@RequestMapping("/update")
	public String updateintro() {
		return "update";
	}
	
	// 여기서부터 테스트입니다.
	@RequestMapping("/testWeb")
	public String testbwe() {
		return "testWeb";
	}
	
		
	// 여기서부터 테스트입니다.
	@RequestMapping("/test2")
	public String testServer() {
		return "index2";
	}
	
	@RequestMapping(value = "/uploadFileToServer", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<?> uploadFileToServer(
			@RequestParam("uploadfile") MultipartFile uploadfile) {
		try {
			String filename = com.bike.webapp.util.UploadFiles.safeName(uploadfile);
			String directory = "/home/ec2-user/data/gpx";
			String filepath = Paths.get(directory, filename).toString();
			
			// Save the file locally
			BufferedOutputStream stream =
					new BufferedOutputStream(new FileOutputStream(new File(filepath)));
			stream.write(uploadfile.getBytes());
			stream.close();
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<>(HttpStatus.OK);
	} 
	// 여기서까지 테스트입니다.
}
