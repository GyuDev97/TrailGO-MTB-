package com.bike.webapp.dto;

import java.sql.Timestamp;

public class Club_Join {
	private String cj_rider;
	private int cj_club;
	private Timestamp cj_join;
	private Timestamp cj_withdraw;
	private int cj_num;

	public String getCj_rider() {
		return cj_rider;
	}

	public void setCj_rider(String cj_rider) {
		this.cj_rider = cj_rider;
	}

	public int getCj_club() {
		return cj_club;
	}

	public void setCj_club(int cj_club) {
		this.cj_club = cj_club;
	}

	public Timestamp getCj_join() {
		return cj_join;
	}

	public void setCj_join(Timestamp cj_join) {
		this.cj_join = cj_join;
	}

	public Timestamp getCj_withdraw() {
		return cj_withdraw;
	}

	public void setCj_withdraw(Timestamp cj_withdraw) {
		this.cj_withdraw = cj_withdraw;
	}

	public int getCj_num() {
		return cj_num;
	}

	public void setCj_num(int cj_num) {
		this.cj_num = cj_num;
	}
}
