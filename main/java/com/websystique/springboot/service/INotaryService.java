package com.websystique.springboot.service;

public interface INotaryService {

	public String sign(String dataToSign);

	public boolean verify(String dataToSign, String signature);
	
	public void rotateSecret();
}
