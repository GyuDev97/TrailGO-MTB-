package com.bike.webapp.dto;

public class Nomtb {
	private int pr_num;
	private String pr_rider;
	private String pr_lattude;
	private String pr_addr;
	private String pr_image;
	private int pr_status;
	private String pr_content;
	private String pr_longitude;
	private String pr_location;
	
	

	public int getPr_num() {
		return pr_num;
	}

	public void setPr_num(int pr_num) {
		this.pr_num = pr_num;
	}

	public String getPr_rider() {
		return pr_rider;
	}

	public void setPr_rider(String pr_rider) {
		this.pr_rider = pr_rider;
	}

	public String getPr_lattude() {
		return pr_lattude;
	}
	public void setPr_lattude(String pr_lattude) {
		this.pr_lattude = pr_lattude;
	}


	public String getPr_addr() {
		return pr_addr;
	}


	public void setPr_addr(String pr_addr) {
		this.pr_addr = pr_addr;
	}


	public String getPr_image() {
		return pr_image;
	}


	public void setPr_image(String pr_image) {
		this.pr_image = pr_image;
	}


	public int getPr_status() {
		return pr_status;
	}


	public void setPr_status(int pr_status) {
		this.pr_status = pr_status;
	}


	public String getPr_content() {
		return pr_content;
	}


	public void setPr_content(String pr_content) {
		this.pr_content = pr_content;
	}


	public String getPr_longitude() {
		return pr_longitude;
	}


	public void setPr_longitude(String pr_longitude) {
		this.pr_longitude = pr_longitude;
	}


	public String getPr_location() {
		return pr_location;
	}


	public void setPr_location(String pr_location) {
		this.pr_location = pr_location;
	}




	@Override
	public String toString() {
		return pr_num+"/"+pr_rider+"/"+pr_lattude+"/"+pr_longitude+"/"+pr_addr+"/"+pr_content+"/"+pr_image;
	}
	

}
