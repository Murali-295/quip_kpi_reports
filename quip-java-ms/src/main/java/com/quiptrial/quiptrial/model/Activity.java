package com.quiptrial.quiptrial.model;

public class Activity {
	private String userName;
	private String domain;
	private String activityName;
	private String createdDate;
	private String updatedDate;
	private String activityCycle;
	public Activity(String userName, String domain, String activityName, String createdDate, String updatedDate, String activityCycle) {
		this.userName=userName;
		this.domain=domain;
		this.activityName=activityName;
		this.createdDate=createdDate;
		this.updatedDate=updatedDate;
		this.activityCycle=activityCycle;
	}
	public Activity() {
			}
	public String getUpdatedDate() {
		return updatedDate;
	}
	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}
	public String getActivityCycle() {
		return activityCycle;
	}
	public void setActivityCycle(String activityCycle) {
		this.activityCycle = activityCycle;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getActivityName() {
		return activityName;
	}
	public void setActivityName(String activityName) {
		this.activityName = activityName;
	}
	public String getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}
}

