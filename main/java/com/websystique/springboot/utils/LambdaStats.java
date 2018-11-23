package com.websystique.springboot.utils;

public class LambdaStats {

    private boolean coldStart;
    private String containerId;

	public boolean getColdStart()
	{
		return this.coldStart;
	}

	public void setColdStart(boolean coldStart)
	{
		this.coldStart = coldStart;
	}

	public String getContainerId()
	{
		return this.containerId;
	}

	public void setContainerId(String containerId)
	{
		this.containerId = containerId;
	}


}