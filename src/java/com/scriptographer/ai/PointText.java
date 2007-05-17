/*
 * Scriptographer
 * 
 * This file is part of Scriptographer, a Plugin for Adobe Illustrator.
 * 
 * Copyright (c) 2002-2007 Juerg Lehni, http://www.scratchdisk.com.
 * All rights reserved.
 *
 * Please visit http://scriptographer.com/ for updates and contact.
 * 
 * -- GPL LICENSE NOTICE --
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * -- GPL LICENSE NOTICE --
 * 
 * File created on 23.10.2005.
 * 
 * $Id$
 */

package com.scriptographer.ai;

/**
 * @author lehni
 */
public class PointText extends TextFrame {

	protected PointText(int handle) {
		super(handle);
	}

	native private static int nativeCreate(short orient, float x, float y);
	
	/**
	 * Creates a point text item
	 * 
	 * @param point the point where the text will begin
	 * @param orient the text orientation, TextFrame.ORIENTATION_*
	 */
	public PointText(Point point, short orient) {
		this(nativeCreate(orient, point.x, point.y));
	}

	public PointText(Point point) {
		this(point, ORIENTATION_HORIZONTAL);
	}

	// read only. AITransformArt suite can be used to change a kPointTextType's
	// anchor.
	public native Point getAnchor();
}
