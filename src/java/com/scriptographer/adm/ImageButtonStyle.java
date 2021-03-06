/*
 * Scriptographer
 *
 * This file is part of Scriptographer, a Scripting Plugin for Adobe Illustrator
 * http://scriptographer.org/
 *
 * Copyright (c) 2002-2010, Juerg Lehni
 * http://scratchdisk.com/
 *
 * All rights reserved. See LICENSE file for details.
 * 
 * File created on Apr 14, 2008.
 */

package com.scriptographer.adm;

import com.scratchdisk.util.IntegerEnum;

/**
 * ADMPictureButtonStyle
 * 
 * @author lehni
 */
public enum ImageButtonStyle implements IntegerEnum {
	BLACK_SUNKEN_RECT(0),
	BLACK_RECT(1);

	protected int value;

	private ImageButtonStyle(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}
}
