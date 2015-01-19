package com.wcities.eventseeker.viewdata;

import java.io.Serializable;


public class SharedElementPosition implements Serializable {
	
	private int startX, startY;
	private int width, height;
	
	public SharedElementPosition(int startX, int startY, int width, int height) {
		this.startX = startX;
		this.startY = startY;
		this.width = width;
		this.height = height;
	}

	public int getStartX() {
		return startX;
	}

	public int getStartY() {
		return startY;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
