package com.bike.webapp.dto;

public class Trade_Info {
	private int status;
	private int cnt;
	
	private int yet, ing, ed;
	
	public int getYet() {
		return yet;
	}
	public void setYet(int yet) {
		this.yet = yet;
	}
	public int getIng() {
		return ing;
	}
	public void setIng(int ing) {
		this.ing = ing;
	}
	public int getEd() {
		return ed;
	}
	public void setEd(int ed) {
		this.ed = ed;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public int getCnt() {
		return cnt;
	}
	public void setCnt(int cnt) {
		this.cnt = cnt;
	}
}
