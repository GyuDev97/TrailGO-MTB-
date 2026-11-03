package com.bike.webapp.dto;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Qna {
	private int qa_num;
	private String qa_title;
	private String qa_content;
	private String qa_answer;
	private Timestamp qa_time;
	private String qa_rider;
	private String qa_shop;
	
	private String shop_time;
	private String rider_time;
	
	
	public String getRider_time() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		rider_time = sdf.format(qa_time);
		return rider_time;
	}
	public void setRider_time(String rider_time) {
		this.rider_time = rider_time;
	}
	public String getShop_time() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		shop_time = sdf.format(qa_time);
		return shop_time;
	}
	public void setShop_time(String shop_time) {
		this.shop_time = shop_time;
	}
	public int getQa_num() {
		return qa_num;
	}
	public void setQa_num(int qa_num) {
		this.qa_num = qa_num;
	}
	public String getQa_title() {
		return qa_title;
	}
	public void setQa_title(String qa_title) {
		this.qa_title = qa_title;
	}
	public String getQa_content() {
		return qa_content;
	}
	public void setQa_content(String qa_content) {
		this.qa_content = qa_content;
	}
	public String getQa_answer() {
		return qa_answer;
	}
	public void setQa_answer(String qa_answer) {
		this.qa_answer = qa_answer;
	}
	public Timestamp getQa_time() {
		return qa_time;
	}
	public void setQa_time(Timestamp qa_time) {
		this.qa_time = qa_time;
	}
	public String getQa_rider() {
		return qa_rider;
	}
	public void setQa_rider(String qa_rider) {
		this.qa_rider = qa_rider;
	}
	public String getQa_shop() {
		return qa_shop;
	}
	public void setQa_shop(String qa_shop) {
		this.qa_shop = qa_shop;
	}
}
