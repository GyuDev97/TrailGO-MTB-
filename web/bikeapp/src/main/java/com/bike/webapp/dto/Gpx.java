package com.bike.webapp.dto;

import java.util.ArrayList;
import java.util.List;

public class Gpx {
	private double maxLat;
	private double minLat;
	private double maxLon;
	private double minLon;
	private int rr_num;
	private String status;
	private List<Info_GPX> infos;
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getRr_num() {
		return rr_num;
	}
	public void setRr_num(int rr_num) {
		this.rr_num = rr_num;
	}
	public double getMaxLat() {
		return maxLat;
	}
	public void setMaxLat(double maxLat) {
		this.maxLat = maxLat;
	}
	public double getMinLat() {
		return minLat;
	}
	public void setMinLat(double minLat) {
		this.minLat = minLat;
	}
	public double getMaxLon() {
		return maxLon;
	}
	public void setMaxLon(double maxLon) {
		this.maxLon = maxLon;
	}
	public double getMinLon() {
		return minLon;
	}
	public void setMinLon(double minLon) {
		this.minLon = minLon;
	}
	public List<Info_GPX> getInfos() {
		return infos;
	}
	public void setInfos(List<Info_GPX> infos) {
		this.infos = infos;
	}
	
	public void setting(String txt) {
		double maxLat,maxLon,minLat,minLon;
		if(txt.contains("maxLat=")) {
			txt = txt.substring(txt.indexOf("maxLat=\"")+("maxLat=\"".length()));
			maxLat = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
			txt = txt.substring(txt.indexOf("maxLon=\"")+"maxLon=\"".length());
			maxLon = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
			txt = txt.substring(txt.indexOf("minLat=\"")+"minLat=\"".length());
			minLat = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
			txt = txt.substring(txt.indexOf("minLon=\"")+"minLon=\"".length());
			minLon = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
		} else if(txt.contains("maxlat=")) {
			txt = txt.substring(txt.indexOf("maxlat=\"")+("maxlat=\"".length()));
			maxLat = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
			txt = txt.substring(txt.indexOf("maxlon=\"")+"maxlon=\"".length());
			maxLon = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
			txt = txt.substring(txt.indexOf("minlat=\"")+"minlat=\"".length());
			minLat = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
			txt = txt.substring(txt.indexOf("minlon=\"")+"minlon=\"".length());
			minLon = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
		} else {
			maxLat = 0;
			maxLon = 0;
			minLat = 0;
			minLon = 0;
		}
		
		this.setMaxLat(maxLat);
		this.setMaxLon(maxLon);
		this.setMinLat(minLat);
		this.setMinLon(minLon);
		
		
		List<Info_GPX> info_list = new ArrayList<Info_GPX>();
		
		while(true) { 
			if(txt.contains("lat=")) {
				Info_GPX info_gpx = new Info_GPX();
				txt = txt.substring(txt.indexOf("lat=\"")+"lat=\"".length());
				double lat = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
				txt = txt.substring(txt.indexOf("lon=\"")+"lon=\"".length());
				double lon = Double.parseDouble(txt.substring(0, txt.indexOf("\"")));
				txt = txt.substring(txt.indexOf("<ele>")+"<ele>".length());
				double ele = Double.parseDouble(txt.substring(0, txt.indexOf("<")));
				info_gpx.setLat(lat);
				info_gpx.setLon(lon);
				info_gpx.setEle(ele);
				info_list.add(info_gpx);
			}else { break; } 
		}
		this.setInfos(info_list);
	}
}
